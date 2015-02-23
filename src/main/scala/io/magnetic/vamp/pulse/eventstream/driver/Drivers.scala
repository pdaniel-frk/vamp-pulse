package io.magnetic.vamp.pulse.eventstream.driver

import akka.actor.{ActorSystem, ActorRef}
import com.sclasen.akka.kafka.{AkkaConsumer, AkkaConsumerProps}
import com.typesafe.config.ConfigFactory
import io.magnetic.vamp.pulse.eventstream.decoder.MetricDecoder
import io.magnetic.vamp.pulse.eventstream.producer.{Metric, SSEMetricsPublisher}
import kafka.serializer.{StringDecoder, DefaultDecoder}
import org.glassfish.jersey.client.{JerseyClientBuilder, JerseyClient}
import org.glassfish.jersey.media.sse.{InboundEvent, EventSource}

trait Driver {
  def start(config: Map[String, String], ref: ActorRef, system: ActorSystem)

  def stop()


}

object SseDriver extends Driver{
  private val client: JerseyClient = JerseyClientBuilder.createClient()

  private var eventSource: EventSource = _
  
  private val decoder = new MetricDecoder()

  override def start(config: Map[String, String], ref: ActorRef, system: ActorSystem): Unit = {
    val target = client.target(config("url"))
    eventSource = new EventSource(target) {
      override def onEvent(inboundEvent: InboundEvent): Unit = inboundEvent.getName match {
        case "metric" => ref ! decoder.fromString(inboundEvent.readData(classOf[String]))
        case _ => println(s"Received event ${inboundEvent.getName}, ignoring")
      }
    }
  }

  override def stop(): Unit = {
    eventSource.close()
  }
}

object KafkaDriver extends Driver {
  var consumer: Option[AkkaConsumer[Array[Byte], Metric]] = Option.empty
  
  override def start(config: Map[String, String], ref: ActorRef, system: ActorSystem): Unit = {
    val consumerProps = AkkaConsumerProps.forSystem(
      system = system,
      zkConnect = config("url"),
      topic = config("topic"),
      group = config("group"),
      streams = config("partitions").toInt, //one per partition
      keyDecoder = new DefaultDecoder(),
      msgDecoder = new MetricDecoder(),
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

