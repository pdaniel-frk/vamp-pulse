package io.magnetic.vamp.pulse.eventstream.driver

import java.util.concurrent.TimeUnit

import io.magnetic.vamp.pulse.eventstream.decoder.ElasticEventDecoder
import org.glassfish.jersey.media.sse.{EventSource, InboundEvent, EventListener}

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorRef, Props, AbstractLoggingActor, Actor}
import org.glassfish.jersey.client.{JerseyClientBuilder, ClientProperties, ClientConfig, JerseyClient}

import scala.concurrent.{Future, Await}
import scala.util.{Try, Failure, Success}

import scala.concurrent.duration._

case object CheckConnection
case object CloseConnection
case object OpenConnection

class SSEConnectionActor(streamUrl: String, producerRef: ActorRef) extends AbstractLoggingActor{
  def client: JerseyClient = {
    val config: ClientConfig  = new ClientConfig()
    config.property(ClientProperties.CONNECT_TIMEOUT, 1000)
    config.property(ClientProperties.READ_TIMEOUT, 1000)

    val c = JerseyClientBuilder.createClient(config)
    c
  }

  private val decoder = new ElasticEventDecoder()

  val target = client.target(streamUrl)

  val listener = new EventListener {
    override def onEvent(inboundEvent: InboundEvent): Unit = inboundEvent.getName match {
      case "metric" => {
        producerRef ! decoder.fromString(inboundEvent.readData(classOf[String]))
      }
      case _ => println(s"Received event ${inboundEvent.getName}, ignoring")
    }
  }

  private var eventSource: Option[EventSource] = Option.empty

  def buildEventSource = {
    val c = JerseyClientBuilder.createClient()
    val es = EventSource.target(c.target(streamUrl)).reconnectingEvery(500, TimeUnit.MILLISECONDS).build()
    es.register(listener)
    Option(es)
  }

  private var isOpen: Boolean = false

  override def receive: Receive = {
    case CloseConnection if isOpen => isOpen = false
      if(eventSource.isDefined && eventSource.get.isOpen) {
        eventSource.get.close()
        eventSource = Option.empty
      }
    case OpenConnection if !isOpen => {
      isOpen = true
      self ! CheckConnection
    }
    case CheckConnection => {
      val fut = Future {
        target.request().head()
      }

      val t = Try(Await.result(fut, 2 seconds))

      if(t.isFailure) {
        if(eventSource.isDefined && eventSource.get.isOpen) {
          eventSource.get.close
          eventSource = Option.empty
        }
        println(t.failed)
        println("Failed to connect")
      } else {
        if(eventSource.isEmpty && isOpen){
          println("build new source")
          eventSource = buildEventSource
          eventSource.get.open()
        }
        println(t.get.getMediaType)
        println("Connected")
      }

      if(isOpen) context.system.scheduler.scheduleOnce(2000 millis, self, CheckConnection)

    }
  }
}

object SSEConnectionActor {
  def props(streamUrl: String, producerRef: ActorRef): Props = Props(new SSEConnectionActor(streamUrl, producerRef))
}
