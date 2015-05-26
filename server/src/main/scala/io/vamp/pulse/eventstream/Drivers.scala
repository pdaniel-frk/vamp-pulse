package io.vamp.pulse.eventstream

import akka.actor.{ActorRef, ActorSystem}
import com.sclasen.akka.kafka.{AkkaConsumer, AkkaConsumerProps}
import com.typesafe.config.ConfigFactory
import io.vamp.pulse.model.Event
import kafka.serializer.DefaultDecoder

trait Driver {

  def start(ref: ActorRef, system: ActorSystem)

  def stop()
}

object SseDriver extends Driver {

  private val config = ConfigFactory.load().getConfig("vamp.pulse.event-stream.sse")

  private var sseActorRef: ActorRef = _

  override def start(ref: ActorRef, system: ActorSystem): Unit = {
    sseActorRef = system.actorOf(SSESupervisionActor.props(config.getString("url"), ref))
    sseActorRef ! OpenConnection
  }

  override def stop(): Unit = {
    sseActorRef ! CloseConnection
  }
}

object KafkaDriver extends Driver {

  private val config = ConfigFactory.load().getConfig("vamp.pulse.event-stream.kafka")

  private var consumer: Option[AkkaConsumer[Array[Byte], Event]] = Option.empty

  override def start(ref: ActorRef, system: ActorSystem): Unit = {
    val consumerProps = AkkaConsumerProps.forSystem(
      system = system,
      zkConnect = config.getString("url"),
      topic = config.getString("topic"),
      group = config.getString("group"),
      streams = config.getString("partitions").toInt, //one per partition
      keyDecoder = new DefaultDecoder(),
      msgDecoder = new EventDecoder(),
      receiver = ref
    )
    consumer = Option(new AkkaConsumer(consumerProps))
    consumer.get.start()
  }

  override def stop(): Unit = {
    consumer match {
      case Some(cons) => cons.stop()
      case None => println("Nothing to stop")
    }
  }
}

