package io.vamp.pulse.eventstream

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import com.typesafe.scalalogging.Logger
import io.vamp.pulse.model.Event
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

object SSEMetricsPublisher {
  def props: Props = Props[SSEMetricsPublisher]

  case object Accepted

  case object Rejected

}

/**
 * This class is a publisher of ElasticEvents from SSE stream
 * It does not have a push-back, since SSE is not something
 * that fits the paradigm, therefore it just drops events
 * if they are not being processed in time and logs them
 */
class SSEMetricsPublisher extends ActorPublisher[Event] {

  import akka.stream.actor.ActorPublisherMessage._

  private val logger = Logger(LoggerFactory.getLogger(classOf[SSEMetricsPublisher]))

  val maxBufSize = 10000
  var buf = Vector.empty[Event]

  override def receive: Receive = {
    case event: Event if buf.size == maxBufSize ⇒
      logger.warn(s"Rejected a message due to buffer overflow: $event")
    case event: Event ⇒
      if (buf.isEmpty && totalDemand > 0)
        onNext(event)
      else {
        buf :+= event
        deliverBuf()
      }
    case Request(_) ⇒ deliverBuf()
    case Cancel ⇒
      context.stop(self)
  }

  @tailrec
  final def deliverBuf(): Unit = {
    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (deliver, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        deliver foreach onNext
      } else {
        val (deliver, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        deliver foreach onNext
        deliverBuf()
      }
    }
  }
}
