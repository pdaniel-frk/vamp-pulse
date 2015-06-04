package io.vamp.pulse.notification

import akka.actor.AbstractLoggingActor
import io.vamp.common.akka.ActorExecutionContextProvider
import io.vamp.common.notification._
import io.vamp.pulse.client.PulseClientProvider


abstract class AbstractPulseNotificationActor(override protected val pulseUrl: String) extends AbstractLoggingActor with NotificationActor with TagResolverProvider with PulseClientProvider with PulseNotificationEventFormatter with ActorExecutionContextProvider {

  override def error(notification: Notification, message: String): Unit = {
    pulseClient.sendEvent(formatNotification(notification, List("notification", "error")))
  }

  override def info(notification: Notification, message: String): Unit = {
    pulseClient.sendEvent(formatNotification(notification, List("notification", "info")))
  }
}
