package io.vamp.pulse.client

import java.time.OffsetDateTime

import io.vamp.common.akka.ExecutionContextProvider
import io.vamp.common.http.RestClient
import io.vamp.common.json.OffsetDateTimeSerializer
import io.vamp.pulse.model._
import org.json4s.DefaultFormats
import org.json4s.ext.EnumNameSerializer

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag


class PulseClient(val url: String)(implicit executionContext: ExecutionContext) {

  def info: Future[Any] = RestClient.request[Any](s"GET $url/api/v1/info")

  def sendEvent(event: Event): Future[Event] = RestClient.request[Event](s"POST $url/api/v1/events", event)

  def getEvents(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime): Future[List[Event]] =
    getEvents(tags, Some(from), Some(to))

  def getEvents(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None): Future[List[Event]] =
    eventQuery[List[Event]](EventQuery(tags = tags, Some(TimeRange(from, to))))

  protected def eventQuery[T <: Any : ClassTag](query: EventQuery)(implicit mf: scala.reflect.Manifest[T]): Future[T] = {
    implicit val formats = DefaultFormats + new OffsetDateTimeSerializer() + new EnumNameSerializer(Aggregator)
    RestClient.request[T](s"POST $url/api/v1/events/get", query)
  }
}

trait PulseClientProvider {
  this: ExecutionContextProvider =>

  protected val pulseUrl: String

  val pulseClient = new PulseClient(pulseUrl)
}
