package io.vamp.pulse.old.eventstream.driver

import java.util.concurrent.TimeUnit

import akka.actor.{AbstractLoggingActor, ActorRef, Props}
import io.vamp.pulse.old.configuration.{PulseActorLoggingNotificationProvider, PulseNotificationActor, TimeoutConfigurationProvider}
import io.vamp.pulse.old.eventstream.decoder.EventDecoder
import io.vamp.pulse.notification.{ConnectionSuccessful, NotStreamError, UnableToConnectError}
import org.glassfish.jersey.client.{ClientConfig, ClientProperties, JerseyClientBuilder}
import org.glassfish.jersey.media.sse.{EventListener, EventSource, InboundEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case object CheckConnection
case object CloseConnection
case object OpenConnection

class SSEConnectionActor(streamUrl: String, producerRef: ActorRef) extends AbstractLoggingActor with PulseActorLoggingNotificationProvider with TimeoutConfigurationProvider {

  override protected val notificationActor: ActorRef = context.actorOf(PulseNotificationActor.props())

  private val decoder = new EventDecoder()




  val target = {
    val conf: ClientConfig  = new ClientConfig()
    conf.property(ClientProperties.CONNECT_TIMEOUT, config.getInt("http.connect"))
    conf.property(ClientProperties.READ_TIMEOUT, config.getInt("http.connect"))

    val httpClient = JerseyClientBuilder.createClient(conf)
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

    case CloseConnection if isOpen =>
      log.debug("closing sse connection")
      isOpen = false
      if(eventSource.isDefined && eventSource.get.isOpen) {
        eventSource.get.close()
        eventSource = Option.empty
      }

    case OpenConnection if !isOpen =>
      log.debug("opening sse connection")
      isOpen = true
      self ! CheckConnection

    case CheckConnection =>
      val result = Try(
        blocking {
          target.request().head()
        }
      )

      result match {

        case Failure(_) =>
          if (eventSource.isDefined && eventSource.get.isOpen) {
            eventSource.get.close
            eventSource = Option.empty
          }
          exception(UnableToConnectError(streamUrl))

        case Success(resp) if resp.getHeaderString("X-Vamp-Stream") != null =>
          if(eventSource.isEmpty && isOpen){
            eventSource = buildEventSource
            eventSource.get.open()
            log.info(message(ConnectionSuccessful(streamUrl)))
          }

        case _ => exception(NotStreamError(streamUrl))
      }


      if(isOpen) context.system.scheduler.scheduleOnce(config.getInt("sse.connection.checkup") millis, self, CheckConnection)


  }
}

object SSEConnectionActor {
  def props(streamUrl: String, producerRef: ActorRef): Props = Props(new SSEConnectionActor(streamUrl, producerRef))
}
