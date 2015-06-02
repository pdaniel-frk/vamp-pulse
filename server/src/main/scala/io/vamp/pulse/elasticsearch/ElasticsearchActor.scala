package io.vamp.pulse.elasticsearch

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import akka.actor._
import akka.util.Timeout
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{IndexDefinition, QueryDefinition, SearchType}
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.Bootstrap.{Shutdown, Start}
import io.vamp.common.akka._
import io.vamp.common.http.RestClient
import io.vamp.common.json.OffsetDateTimeSerializer
import io.vamp.common.vitals.InfoRequest
import io.vamp.pulse.http.PulseSerializationFormat
import io.vamp.pulse.model._
import io.vamp.pulse.notification.{AggregatorNotSupported, EmptyEventError, MappingErrorNotification, PulseNotificationProvider}
import org.elasticsearch.index.mapper.MapperParsingException
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.RemoteTransportException
import org.json4s.native.JsonMethods._

import scala.collection.Seq
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object ElasticsearchActor extends ActorDescription {

  val configuration = ConfigFactory.load().getConfig("vamp.pulse")

  val timeout = Timeout(configuration.getInt("elasticsearch.response-timeout") seconds)

  def props(args: Any*): Props = Props[ElasticsearchActor]

  case class Index(event: Event)

  case class BatchIndex(events: Seq[Event])

  case class Search(query: EventQuery)

}

class ElasticsearchActor extends CommonActorSupport with PulseNotificationProvider {

  import CustomObjectSource._
  import ElasticsearchActor._
  import OffsetDateTimeSerializer._

  implicit val timeout = ElasticsearchActor.timeout

  private val indexConfiguration = configuration.getConfig("elasticsearch.index")

  private val defaultIndex = indexConfiguration.getString("name")

  private lazy val elasticsearch = if (configuration.getString("elasticsearch.type").toLowerCase == "embedded")
    new EmbeddedElasticsearchServer(configuration.getConfig("elasticsearch.embedded"))
  else
    new RemoteElasticsearchServer(configuration.getConfig("elasticsearch.remote"))

  private val url = configuration.getString("elasticsearch.url")

  def receive: Receive = {

    case InfoRequest => info()

    case BatchIndex(events) => replyWith(insertEvent(events) map { _ => events })

    case Index(event) => replyWith(insertEvent(event) map { _ => event })

    case Search(query) => replyWith(queryEvents(query))

    case Start => start()

    case Shutdown => shutdown()
  }

  private def info() = {
    val receiver = sender()
    RestClient.request[Any](s"GET $url").map(response => receiver ! response)
  }

  private def start() = {
    elasticsearch.start()

    elasticsearch.client.execute {
      get template defaultIndex
    } map { response =>
      if (response.getIndexTemplates.isEmpty) {
        val template = Source.fromInputStream(getClass.getResourceAsStream("template.json")).mkString.replace("$NAME", defaultIndex)
        RestClient.request[Any](s"PUT $url/_template/$defaultIndex", template)
      }
    }
  }

  private def shutdown() = {
    elasticsearch.shutdown()
  }

  private def replyWith(callback: => Future[_]): Unit = try {
    sender ! offload(callback)
  } catch {
    case e: Exception => sender ! e
  }

  private def insertEvent(event: Event) = {
    if (event.tags.isEmpty) error(EmptyEventError)

    elasticsearch.client.execute {
      insertQuery(event)
    } recoverWith {
      case e: RemoteTransportException => e.getCause match {
        case t: MapperParsingException => error(MappingErrorNotification(e.getCause, event.`type`))
      }
    }
  }

  private def insertEvent(eventList: Seq[Event]) = {
    elasticsearch.client.execute {
      bulk(
        eventList.filter(_.tags.nonEmpty).map(event => insertQuery(event))
      )
    }
  }

  private def insertQuery(event: Event): IndexDefinition = {
    val schema = event.`type`

    val time = {
      val path = s"time-format.$schema"
      val format = indexConfiguration.getString(if (indexConfiguration.hasPath(path)) path else "event")
      OffsetDateTime.now().format(DateTimeFormatter.ofPattern(format))
    }

    index into(s"$defaultIndex-$schema-$time", schema) doc event
  }

  private def queryEvents(eventQuery: EventQuery): Future[_] = {
    eventQuery.aggregator match {
      case None => getEvents(eventQuery)
      case Some(Aggregator(Some(Aggregator.`count`), _)) => countEvents(eventQuery)
      case Some(aggregator) => aggregateEvents(eventQuery)
    }
  }

  private def getEvents(eventQuery: EventQuery, eventLimit: Int = 30) = {
    searchEvents(eventQuery, eventLimit) map {
      response =>
        implicit val formats = PulseSerializationFormat.deserializer
        response.getHits.hits().map(hit => parse(hit.sourceAsString()).extract[Event]).toList
    }
  }

  private def countEvents(eventQuery: EventQuery) = {
    searchEvents(eventQuery, 0) map {
      response => SingleValueAggregationResult(response.getHits.totalHits())
    }
  }

  private def searchEvents(eventQuery: EventQuery, eventLimit: Int) = {
    elasticsearch.client.execute {
      search in defaultIndex query {
        must(constructQuery(eventQuery))
      } sort (by field "timestamp" order SortOrder.DESC) start 0 limit eventLimit
    }
  }

  private def constructQuery(eventQuery: EventQuery): List[QueryDefinition] = {
    val tagNum = eventQuery.tags.size
    val queries = constructTimeQuery(eventQuery.time) :: Nil

    if (tagNum == 0) queries
    else
      queries :+ (termsQuery("tags", eventQuery.tags.toSeq: _*) minimumShouldMatch tagNum)
  }

  private def constructTimeQuery(timeRange: Option[TimeRange]) = {
    timeRange match {
      case Some(TimeRange(Some(from), Some(to), lower, upper)) =>
        rangeQuery("timestamp") from toUnixMicro(from) includeLower lower to toUnixMicro(to) includeUpper upper

      case Some(TimeRange(None, Some(to), _, upper)) =>
        rangeQuery("timestamp") to toUnixMicro(to) includeUpper upper

      case Some(TimeRange(Some(from), None, lower, _)) =>
        rangeQuery("timestamp") from toUnixMicro(from) includeLower lower

      case _ => rangeQuery("timestamp")
    }
  }

  private def aggregateEvents(eventQuery: EventQuery) = {
    val aggregator = eventQuery.aggregator.get
    val aggregationField = List("value", aggregator.field.getOrElse("")).filter(p => !p.isEmpty).mkString(".")

    elasticsearch.client.execute {
      search in defaultIndex searchType SearchType.Count aggs {
        aggregation filter "filter_agg" filter {
          queryFilter(must(constructQuery(eventQuery)))
        } aggs {
          aggregator.`type` match {
            case Some(Aggregator.`average`) => aggregation avg "val_agg" field aggregationField
            case Some(Aggregator.`min`) => aggregation min "val_agg" field aggregationField
            case Some(Aggregator.`max`) => aggregation max "val_agg" field aggregationField
            case _ => error(AggregatorNotSupported())
          }
        }
      }
    } map {
      response =>
        val value: Double = response.getAggregations
          .get("filter_agg").asInstanceOf[InternalFilter]
          .getAggregations.get("val_agg").asInstanceOf[InternalNumericMetricsAggregation.SingleValue]
          .value()

        SingleValueAggregationResult(if (value.isNaN || value.isInfinite) 0D else value)
    }
  }
}
