package io.magnetic.vamp.pulse.eventstream.producer

import akka.actor.Props
import akka.stream.actor.ActorPublisher

import scala.annotation.tailrec

object SSEMetricsPublisher {
  def props: Props = Props[SSEMetricsPublisher]



  case object Accepted
  case object Rejected
}

class SSEMetricsPublisher extends ActorPublisher[Event]  {
  import akka.stream.actor.ActorPublisherMessage._

  val maxBufSize = 1000
  var buf = Vector.empty[Event]

  override def receive: Receive = {
    case metric: Event if buf.size == maxBufSize =>
//      sender ! Rejected
      println(s"Rejected a message due to buffer overflow: $metric")
    case metric: Event =>
//      sender ! Accepted
      if(buf.isEmpty && totalDemand > 0)
        onNext(metric)
      else {
        buf :+= metric
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
