package io.magnetic.vamp.pulse.eventstream.producer

import akka.actor.{Props, ActorRef}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import com.sclasen.akka.kafka.StreamFSM

import scala.annotation.tailrec


object KafkaMetricsManager {
  def props: Props = Props[KafkaMetricsManager]
}

class KafkaMetricsManager extends ActorPublisher[Metric]{

  val maxBufSize = 1000
  var buf = Vector.empty[(ActorRef, Metric)]

  override def receive: Receive = {
    case met: String =>
      val metric = Metric(met)
      if(buf.isEmpty && totalDemand > 0) {
        onNext(metric)
        sender() ! StreamFSM.Processed
      } else {
        buf :+= (sender(), metric)
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
