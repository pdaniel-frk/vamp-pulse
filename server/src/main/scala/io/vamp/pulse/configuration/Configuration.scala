package io.vamp.pulse.configuration

import akka.actor.{AbstractLoggingActor, Props}
import com.typesafe.config.ConfigFactory
import io.vamp.common.config.ConfigurationProvider
import io.vamp.common.notification.{DefaultPackageMessageResolverProvider, LoggingNotificationProvider, _}

object DefaultNotification extends LoggingNotificationProvider with DefaultPackageMessageResolverProvider {

}

trait PulseTagResolverProvider extends DefaultTagResolverProvider {
  override def resolveTags(notification: Notification): List[String] = List("pulse") ++ super.resolveTags(notification)
}

class PulseNotificationActor(url: String) extends AbstractPulseNotificationActor(url) with PulseTagResolverProvider with DefaultNotificationEventFormatter {

}

trait PulseActorLoggingNotificationProvider extends ActorLoggingNotificationProvider with DefaultPackageMessageResolverProvider {
  this: AbstractLoggingActor with MessageResolverProvider =>
}

trait TimeoutConfigurationProvider extends ConfigurationProvider {
  override protected val confPath: String = "pulse.timeout"
}

object PulseNotificationActor {
  private val config = ConfigFactory.load()
  def props(): Props = Props(new PulseNotificationActor(config.getString("notification.pulse.url")))
}
