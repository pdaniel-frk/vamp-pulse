package io.vamp.pulse.elasticsearch

import java.io.StringWriter

import akka.actor._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.IndexDefinition
import com.sksamuel.elastic4s.mappings.FieldType.{DateType, ObjectType, StringType}
import com.sksamuel.elastic4s.source.DocumentSource
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.Bootstrap.{Shutdown, Start}
import io.vamp.common.akka._
import io.vamp.common.http.RestClient
import io.vamp.common.vitals.InfoRequest
import io.vamp.pulse.elasticsearch.ElasticsearchActor.{BatchIndex, Index, Search}
import io.vamp.pulse.http.PulseSerializationFormat
import io.vamp.pulse.model.{Event, EventQuery}
import io.vamp.pulse.notification.{EmptyEventError, MappingErrorNotification, PulseNotificationProvider}
import org.elasticsearch.index.mapper.MapperParsingException
import org.elasticsearch.transport.RemoteTransportException

import scala.collection.Seq
import scala.language.{implicitConversions, postfixOps}

object ElasticsearchActor extends ActorDescription {

  def props(args: Any*): Props = Props[ElasticsearchActor]

  case class Index(event: Event)

  case class BatchIndex(events: Seq[Event])

  case class Search(query: EventQuery)

}

class ElasticsearchActor extends CommonActorSupport with PulseNotificationProvider {

  import CustomObjectSource._

  private val eventEntity = "event"
  private val eventIndex = "events"

  implicit val formats = PulseSerializationFormat.serializer

  private val configuration = ConfigFactory.load().getConfig("vamp.pulse")

  private lazy val elasticsearch = if (configuration.getString("elasticsearch.type").toLowerCase == "embedded")
    new EmbeddedElasticsearchServer(configuration.getConfig("elasticsearch.embedded"))
  else
    new RemoteElasticsearchServer(configuration.getConfig("elasticsearch.remote"))

  def receive: Receive = {
    case Start =>
      elasticsearch.start()
      createIndex

    case Shutdown =>
      elasticsearch.shutdown()

    case InfoRequest =>
      sender ! elasticsearch.info

    case BatchIndex(events) =>
      insert(events)
      sender ! events

    case Index(event) =>
      insert(event)
      sender ! event

    case Search(query) =>
      println(s"query: $query")
      sender ! query
  }

  private def createIndex = elasticsearch.client.execute {
    create index eventIndex mappings (
      eventEntity as(
        "tags" typed StringType index NotAnalyzed,
        "timestamp" typed DateType,
        "value" typed ObjectType,
        "blob" typed ObjectType enabled false
        )
      )
  }

  private def insert(event: Event) = {
    if (event.tags.isEmpty) error(EmptyEventError)

    elasticsearch.client.execute {
      insertQuery(event)
    } recoverWith {
      case e: RemoteTransportException => e.getCause match {
        case t: MapperParsingException => error(MappingErrorNotification(e.getCause, event.schema.getOrElse("")))
      }
    }
  }

  private def insert(eventList: Seq[Event]) = {
    elasticsearch.client.execute {
      bulk(
        eventList.filter(_.tags.nonEmpty).map(event => insertQuery(event))
      )
    } await
  }

  private def insertQuery(event: Event): IndexDefinition = {
    index into "$eventIndex/$eventEntity" doc event
  }
}

class CustomObjectSource(any: Any) extends DocumentSource {
  override def json: String = CustomObjectSource.mapper.writeValueAsString(any)
}

object CustomObjectSource {
  val mapper = new ObjectMapper
  mapper.findAndRegisterModules()
  mapper.registerModule(DefaultScalaModule)

  def apply(any: Any) = new CustomObjectSource(any)

  implicit def anyToObjectSource(any: Any): CustomObjectSource = apply(any)
}
