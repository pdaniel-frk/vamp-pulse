package io.magnetic.vamp.pulse.eventstream.driver

import java.util.concurrent.TimeUnit
import org.glassfish.jersey.client.ClientConfig

import akka.actor.{ActorRef, ActorSystem}
import com.sclasen.akka.kafka.{AkkaConsumer, AkkaConsumerProps}
import com.typesafe.config.ConfigFactory
import io.magnetic.vamp.pulse.eventstream.decoder.ElasticEventDecoder
import io.magnetic.vamp.pulse.eventstream.message.ElasticEvent
import kafka.serializer.DefaultDecoder
import org.glassfish.jersey.client.{ClientProperties, JerseyClient, JerseyClientBuilder}
import org.glassfish.jersey.media.sse.{EventListener, EventSource, InboundEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Driver {
  protected val config = ConfigFactory.load().getConfig("stream")

  var sseActorRef: ActorRef = _

  def start(ref: ActorRef, system: ActorSystem)
  def stop()
}

object SseDriver extends Driver{
  lazy private val client: JerseyClient = {
    val config: ClientConfig  = new ClientConfig()
    config.property(ClientProperties.READ_TIMEOUT, 1000)
    config.property(ClientProperties.CONNECT_TIMEOUT, 1000)

    val c = JerseyClientBuilder.createClient(config)

    c
  }

  override def start(ref: ActorRef, system: ActorSystem): Unit = {
    sseActorRef = system.actorOf(SSEConnectionActor.props(config.getString("url"), ref))
    sseActorRef ! OpenConnection
  }

  override def stop(): Unit = {
    sseActorRef ! CloseConnection
  }
}

object KafkaDriver extends Driver {
  var consumer: Option[AkkaConsumer[Array[Byte], ElasticEvent]] = Option.empty
  
  override def start(ref: ActorRef, system: ActorSystem): Unit = {
    val consumerProps = AkkaConsumerProps.forSystem(
      system = system,
      zkConnect = config.getString("url"),
      topic = config.getString("topic"),
      group = config.getString("group"),
      streams = config.getString("partitions").toInt, //one per partition
      keyDecoder = new DefaultDecoder(),
      msgDecoder = new ElasticEventDecoder(),
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

