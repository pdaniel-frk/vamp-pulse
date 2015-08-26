package io.vamp.pulse.eventstream

import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{ PropsSource, Sink, Source }
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.Bootstrap.{ Shutdown, Start }
import io.vamp.common.akka.CommonSupportForActors
import io.vamp.common.akka.IoC._
import io.vamp.common.vitals.InfoRequest
import io.vamp.pulse.elasticsearch.ElasticsearchActor
import io.vamp.pulse.model.Event
import io.vamp.pulse.notification.{ NoEventStreamDriver, PulseNotificationProvider }

import scala.concurrent.duration._
import scala.language.postfixOps

class EventStreamActor extends CommonSupportForActors with PulseNotificationProvider {

  private val configuration = ConfigFactory.load().getConfig("vamp.pulse.event-stream")

  private lazy val (eventManagerSource: Option[PropsSource[Event]], driver: Option[Driver]) = initializeSourceAndDriver

  private implicit val mat = ActorFlowMaterializer()

  def receive: Receive = {
    case Start ⇒ (eventManagerSource, driver) match {
      case (Some(source), Some(d)) ⇒
        val materializedMap = source.groupedWithin(1000, 1 millis)
          .map { events ⇒ actorFor[ElasticsearchActor] ! ElasticsearchActor.BatchIndex(events); events }
          .to(Sink.ignore).run()

        d.start(materializedMap.get(source), context.system)

      case _ ⇒
    }

    case Shutdown ⇒
      driver.foreach(_.stop())

    case InfoRequest ⇒
      sender ! ("driver" -> configuration.getString("driver"))
  }

  private def initializeSourceAndDriver = configuration.getString("driver") match {
    case "sse"   ⇒ (Some(Source[Event](SSEMetricsPublisher.props)), Some(SseDriver))
    case "kafka" ⇒ (Some(Source[Event](KafkaMetricsPublisher.props)), Some(KafkaDriver))
    case driver: String ⇒
      info(NoEventStreamDriver(driver))
      (None, None)
  }
}
