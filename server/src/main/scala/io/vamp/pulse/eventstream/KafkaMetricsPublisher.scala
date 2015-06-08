package io.vamp.pulse.eventstream

import akka.actor.{ActorRef, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import com.sclasen.akka.kafka.StreamFSM
import io.vamp.pulse.model.Event

import scala.annotation.tailrec


object KafkaMetricsPublisher {
  def props: Props = Props[KafkaMetricsPublisher]
}

class KafkaMetricsPublisher extends ActorPublisher[Event] {

  var buf = Vector.empty[(ActorRef, Event)]

  override def receive: Receive = {
    case event: Event =>
      if (buf.isEmpty && totalDemand > 0) {
        onNext(event)
        sender() ! StreamFSM.Processed
      } else {
        buf :+= sender() -> event
        deliverBuf()
      }
    case Request(_) => deliverBuf()
    case Cancel =>
      context.stop(self)
  }

  @tailrec
  final def deliverBuf(): Unit = {
    if (totalDemand > 0) {
      if (totalDemand <= Int.MaxValue) {
        val (deliver, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        deliver foreach ((tuple) => {
          onNext(tuple._2)
          tuple._1 ! StreamFSM.Processed
        })
      } else {
        val (deliver, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        deliver foreach ((tuple) => {
          onNext(tuple._2)
          tuple._1 ! StreamFSM.Processed
        })
        deliverBuf()
      }
    }
  }
}
