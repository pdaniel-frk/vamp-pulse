package io.vamp.pulse.elasticsearch

import akka.actor._
import akka.util.Timeout
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{IndexDefinition, QueryDefinition}
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.Bootstrap.{Shutdown, Start}
import io.vamp.common.akka._
import io.vamp.common.vitals.InfoRequest
import io.vamp.pulse.http.PulseSerializationFormat
import io.vamp.pulse.model.{Event, EventQuery, TimeRange}
import io.vamp.pulse.notification.{EmptyEventError, MappingErrorNotification, PulseNotificationProvider}
import org.elasticsearch.index.mapper.MapperParsingException
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

    case Search(query) =>
      replyWith(searchEvent(query))

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

  private def searchEvent(eventQuery: EventQuery) = {
    elasticsearch.client.execute {
      search in indexName -> "event" query {
        must(constructQuery(eventQuery))
      } sort (by field "timestamp" order SortOrder.DESC) start 0 limit 30
    } map {
      response =>
        implicit val formats = PulseSerializationFormat.deserializer
        response.getHits.hits().map(hit => parse(hit.sourceAsString()).extract[Event]).toList
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
}
