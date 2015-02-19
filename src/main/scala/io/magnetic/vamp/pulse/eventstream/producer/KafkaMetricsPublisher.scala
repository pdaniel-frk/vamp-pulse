package io.magnetic.vamp.pulse.eventstream.producer

import akka.actor.{Props, ActorRef}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import com.sclasen.akka.kafka.StreamFSM

import scala.annotation.tailrec


object KafkaMetricsPublisher {
  def props: Props = Props[KafkaMetricsPublisher]
}

class KafkaMetricsPublisher extends ActorPublisher[Metric]{

  var buf = Vector.empty[(ActorRef, Metric)]

  override def receive: Receive = {
    case met: Metric =>
      if(buf.isEmpty && totalDemand > 0) {
        onNext(met)
        sender() ! StreamFSM.Processed
      } else {
        buf :+= (sender(), met)
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
        deliver foreach((tuple) => {
          onNext(tuple._2)
          tuple._1 ! StreamFSM.Processed
        })
      } else {
        val(deliver, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        deliver foreach((tuple) => {
          onNext(tuple._2)
          tuple._1 ! StreamFSM.Processed
        })        
        deliverBuf()
      }
    }
  }
}
