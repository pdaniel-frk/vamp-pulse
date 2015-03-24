package io.vamp.pulse.eventstream.driver

import java.util.concurrent.TimeUnit

import akka.actor.{AbstractLoggingActor, ActorRef, Props}
import akka.util.Timeout
import io.vamp.common.akka.FutureSupport
import io.vamp.pulse.configuration.{PulseActorLoggingNotificationProvider, PulseNotificationActor}
import io.vamp.pulse.eventstream.decoder.ElasticEventDecoder
import io.vamp.pulse.eventstream.notification.{ConnectionSuccessful, NotStream, UnableToConnect}
import org.glassfish.jersey.client.{ClientConfig, ClientProperties, JerseyClientBuilder}
import org.glassfish.jersey.media.sse.{EventListener, EventSource, InboundEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

case object CheckConnection
case object CloseConnection
case object OpenConnection

class SSEConnectionActor(streamUrl: String, producerRef: ActorRef) extends AbstractLoggingActor with PulseActorLoggingNotificationProvider with FutureSupport{

  override protected val notificationActor: ActorRef = context.actorOf(PulseNotificationActor.props())

  private implicit val timeout = Timeout(5 seconds)

  private val decoder = new ElasticEventDecoder()


  val target = {
    val config: ClientConfig  = new ClientConfig()
    config.property(ClientProperties.CONNECT_TIMEOUT, 1000)
    config.property(ClientProperties.READ_TIMEOUT, 1000)

    val httpClient = JerseyClientBuilder.createClient(config)
    httpClient.target(streamUrl)
  }

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

      Await.ready(fut, 2 seconds)

      fut.value.get match {

        case Failure(_) =>
          if (eventSource.isDefined && eventSource.get.isOpen) {
            eventSource.get.close
            eventSource = Option.empty
          }
          exception(UnableToConnect(streamUrl))

        case Success(resp) if resp.getHeaderString("X-Vamp-Stream") != null =>
          if(eventSource.isEmpty && isOpen){
            eventSource = buildEventSource
            eventSource.get.open()
            log.info(message(ConnectionSuccessful(streamUrl)))
          }

        case _ => exception(NotStream(streamUrl))
      }


      if(isOpen) context.system.scheduler.scheduleOnce(2000 millis, self, CheckConnection)

    }
  }
}

object SSEConnectionActor {
  def props(streamUrl: String, producerRef: ActorRef): Props = Props(new SSEConnectionActor(streamUrl, producerRef))
}
