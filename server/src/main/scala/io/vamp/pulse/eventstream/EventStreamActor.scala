package io.vamp.pulse.eventstream

import akka.actor.Props
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{PropsSource, Sink, Source}
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.Bootstrap.{Shutdown, Start}
import io.vamp.common.akka.{ActorDescription, CommonActorSupport}
import io.vamp.common.vitals.InfoRequest
import io.vamp.pulse.elastic.ElasticsearchActor
import io.vamp.pulse.model.Event
import io.vamp.pulse.notification.{NoEventStreamDriver, PulseNotificationProvider}

import scala.concurrent.duration._
import scala.language.postfixOps

object EventStreamActor extends ActorDescription {
  def props(args: Any*): Props = Props[EventStreamActor]
}

class EventStreamActor extends CommonActorSupport with PulseNotificationProvider {

  private val configuration = ConfigFactory.load().getConfig("vamp.pulse.event-stream")

  private lazy val (eventManagerSource: PropsSource[Event], driver: Driver) = initializeSourceAndDriver

  private implicit val mat = ActorFlowMaterializer()

  def receive: Receive = {
    case Start =>

      val materializedMap = eventManagerSource.groupedWithin(1000, 1 millis)
        .map { events => actorFor(ElasticsearchActor) ! ElasticsearchActor.BatchIndex(events); events }
        .to(Sink.ignore).run()

      driver.start(materializedMap.get(eventManagerSource), context.system)

    case Shutdown =>
      driver.stop()

    case InfoRequest =>
      sender ! ("driver" -> configuration.getString("driver"))
  }

  private def initializeSourceAndDriver = configuration.getString("driver") match {
    case "sse" => (Source[Event](SSEMetricsPublisher.props), SseDriver)
    case "kafka" => (Source[Event](KafkaMetricsPublisher.props), KafkaDriver)
    case driver: String => error(NoEventStreamDriver(driver))
  }
}
