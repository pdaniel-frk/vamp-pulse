package io.vamp.pulse.eventstream.producer

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import io.vamp.pulse.eventstream.message.ElasticEvent

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
class SSEMetricsPublisher extends ActorPublisher[ElasticEvent]  {
  import akka.stream.actor.ActorPublisherMessage._

  val maxBufSize = 10000
  var buf = Vector.empty[ElasticEvent]

  override def receive: Receive = {
    case event: ElasticEvent if buf.size == maxBufSize =>
      println(s"Rejected a message due to buffer overflow: $event")
    case event: ElasticEvent =>
      if(buf.isEmpty && totalDemand > 0)
        onNext(event)
      else {
        buf :+= event
        deliverBuf()
      }
    case Request(_) => deliverBuf()
    case Cancel =>
      context.stop(self)
  }

  @tailrec
  final def deliverBuf() : Unit = {
    if(totalDemand > 0) {
      if(totalDemand <= Int.MaxValue) {
        val(deliver, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        deliver foreach onNext
      } else {
        val(deliver, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        deliver foreach onNext
        deliverBuf()
      }
    }
  }
}
