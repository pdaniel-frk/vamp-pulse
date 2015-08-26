package io.vamp.pulse.notification

import akka.actor.{ AbstractLoggingActor, Props }
import com.typesafe.config.ConfigFactory
import io.vamp.common.notification.{ DefaultPackageMessageResolverProvider, LoggingNotificationProvider, _ }

trait PulseNotificationProvider extends LoggingNotificationProvider with DefaultPackageMessageResolverProvider

trait PulseTagResolverProvider extends DefaultTagResolverProvider {
  override def resolveTags(notification: Notification): List[String] = List("pulse") ++ super.resolveTags(notification)
}

trait PulseActorLoggingNotificationProvider extends ActorLoggingNotificationProvider with DefaultPackageMessageResolverProvider {
  this: AbstractLoggingActor with MessageResolverProvider ⇒
}

object PulseNotificationActor {

  private val config = ConfigFactory.load()

  def props(): Props = Props(new PulseNotificationActor(s"${config.getString("vamp.pulse.rest-api.host")}:${config.getString("vamp.pulse.rest-api.port")}"))
}

class PulseNotificationActor(url: String) extends AbstractPulseNotificationActor(url) with PulseTagResolverProvider with DefaultNotificationEventFormatter {
}

