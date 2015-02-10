package io.magnetic.vamp.pulse.eventstream.driver

import akka.actor.ActorRef
import com.typesafe.config.ConfigFactory
import io.magnetic.vamp.pulse.eventstream.producer.MetricsManager
import org.glassfish.jersey.client.{JerseyClientBuilder, JerseyClient}
import org.glassfish.jersey.media.sse.{InboundEvent, EventSource}

trait Driver {
  def start(url: String, ref: ActorRef)

  def stop()

  lazy val url = ConfigFactory.load()

}

object SseDriver extends Driver{
  private val client: JerseyClient = JerseyClientBuilder.createClient()

  private var eventSource: EventSource = _

  override def start(url: String, ref: ActorRef): Unit = {
    val target = client.target(url)
    eventSource = new EventSource(target) {
      override def onEvent(inboundEvent: InboundEvent): Unit = inboundEvent.getName match {
        case "metric" => ref ! MetricsManager.Metric(inboundEvent.readData(classOf[String]))
        case _ => println(s"Received event ${inboundEvent.getName}, ignoring")
      }
    }
  }

  override def stop(): Unit = {
    eventSource.close()
  }
}

