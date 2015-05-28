package io.vamp.pulse.elasticsearch

import akka.actor._
import akka.util.Timeout
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{IndexDefinition, QueryDefinition, SearchType}
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.Bootstrap.{Shutdown, Start}
import io.vamp.common.akka._
import io.vamp.common.vitals.InfoRequest
import io.vamp.pulse.api.AggregatorType
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

  implicit val timeout = ElasticsearchActor.timeout

  private val indexName = configuration.getString("elasticsearch.index-name")

  private lazy val elasticsearch = if (configuration.getString("elasticsearch.type").toLowerCase == "embedded")
    new EmbeddedElasticsearchServer(configuration.getConfig("elasticsearch.embedded"))
  else
    new RemoteElasticsearchServer(configuration.getConfig("elasticsearch.remote"))

  def receive: Receive = {

    case InfoRequest => sender ! elasticsearch.info

    case BatchIndex(events) => replyWith(insertEvent(events) map { _ => events })

    case Index(event) => replyWith(insertEvent(event) map { _ => event })

    case Search(query) => replyWith(queryEvents(query))

    case Start => elasticsearch.start()

    case Shutdown => elasticsearch.shutdown()
  }

  def replyWith(reply: Future[_]) = sender ! offload(reply)

  private def insertEvent(event: Event) = {
    if (event.tags.isEmpty) error(EmptyEventError)

    elasticsearch.client.execute {
      insertQuery(event)
    } recoverWith {
      case e: RemoteTransportException => e.getCause match {
        case t: MapperParsingException => error(MappingErrorNotification(e.getCause, event.`type`.getOrElse("")))
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
    index into s"$indexName/event" doc event
  }

  private def queryEvents(eventQuery: EventQuery) = {
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
      search in indexName -> "event" query {
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

  private def constructTimeQuery(timeRange: Option[TimeRange]) = timeRange match {
    case Some(TimeRange(Some(from), Some(to))) => rangeQuery("timestamp") from from.toEpochSecond to to.toEpochSecond
    case Some(TimeRange(None, Some(to))) => rangeQuery("timestamp") to to.toEpochSecond
    case Some(TimeRange(Some(from), None)) => rangeQuery("timestamp") from from.toEpochSecond
    case _ => rangeQuery("timestamp")
  }

  private def aggregateEvents(eventQuery: EventQuery) = {
    val aggregator = eventQuery.aggregator.get
    val aggregationField = List("value", aggregator.field.getOrElse("")).filter(p => !p.isEmpty).mkString(".")

    elasticsearch.client.execute {
      search in indexName -> "event" searchType SearchType.Count aggs {
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
