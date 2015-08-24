package io.vamp.pulse.eventstream

import akka.actor.{ActorRef, ActorSystem}
import com.sclasen.akka.kafka.{AkkaConsumer, AkkaConsumerProps}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.vamp.common.akka.IoC
import io.vamp.pulse.model.Event
import kafka.serializer.DefaultDecoder
import org.slf4j.LoggerFactory

trait Driver {

  def start(ref: ActorRef, system: ActorSystem)

  def stop()
}

object SseDriver extends Driver {

  private val config = ConfigFactory.load().getConfig("vamp.pulse.event-stream.sse")

  private var sseActorRef: ActorRef = _

  override def start(ref: ActorRef, system: ActorSystem): Unit = {
    implicit val actorSystem = system
    sseActorRef = IoC.createActor(SSESupervisionActor, config.getString("url"))
    sseActorRef ! OpenConnection
  }

  override def stop(): Unit = {
    sseActorRef ! CloseConnection
  }
}

object KafkaDriver extends Driver {

  private val logger = Logger(LoggerFactory.getLogger(KafkaDriver.getClass))

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
      case None => logger.info("Nothing to stop")
    }
  }
}

