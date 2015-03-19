package io.vamp.pulse.configuration

import akka.actor.{AbstractLoggingActor, Actor, Props}
import com.typesafe.config.ConfigFactory
import io.vamp.common.notification._

import io.vamp.common.notification.{DefaultPackageMessageResolverProvider, LoggingNotificationProvider}

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

object PulseNotificationActor {
  private val config = ConfigFactory.load()
  def props(): Props = Props(new PulseNotificationActor(config.getString("notification.pulse.url")))
}
