package io.magnetic.vamp.pulse.eventstream.producer

import akka.actor.Actor.Receive
import akka.actor.Props
import akka.stream.actor.ActorPublisher

import scala.annotation.tailrec

object SSEMetricsPublisher {
  def props: Props = Props[SSEMetricsPublisher]



  case object Accepted
  case object Rejected
}

class SSEMetricsPublisher extends ActorPublisher[Metric] {
  import akka.stream.actor.ActorPublisherMessage._
  import SSEMetricsPublisher._

  val maxBufSize = 1000
  var buf = Vector.empty[Metric]

  override def receive: Receive = {
    case metric: Metric if buf.size == maxBufSize =>
      sender ! Rejected
      println(s"Rejected a message due to buffer overflow: $metric")
    case metric: Metric =>
      sender ! Accepted
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
