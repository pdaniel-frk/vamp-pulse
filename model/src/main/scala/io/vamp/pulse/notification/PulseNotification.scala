package io.vamp.pulse.notification

import java.time.OffsetDateTime

import io.vamp.common.notification._
import io.vamp.pulse.model.Event


//
//
//trait PulseEvent {
//  def tags: List[String] = Nil
//
//  def schema: String = ""
//
//  def value: AnyRef = this
//}
//object DefaultPulseNotificationActor extends ActorDescription {
//
//  def props(args: Any*): Props = Props[DefaultPulseNotificationActor]
//
//}

//class DefaultPulseNotificationActor(override protected val url: String) extends AbstractPulseNotificationActor(url) with DefaultTagResolverProvider with DefaultNotificationEventFormatter {
//}
//
//trait PulseLoggingNotificationProvider extends LoggingNotificationProvider with TagResolverProvider with PulseClientProvider with DefaultNotificationEventFormatter {
//  this: MessageResolverProvider =>
//
//  override def info(notification: Notification): Unit = {
//    client.sendEvent(formatNotification(notification, List("notification", "info")))
//    super.info(notification)
//  }
//
//  override def exception(notification: Notification): Exception = {
//    client.sendEvent(formatNotification(notification, List("notification", "error")))
//    super.exception(notification)
//  }
//}

trait PulseNotificationEventFormatter {
  def formatNotification(notification: Notification, tags: List[String] = List.empty): Event
}

trait DefaultNotificationEventFormatter extends PulseNotificationEventFormatter with TagResolverProvider {
  override def formatNotification(notification: Notification, tags: List[String]): Event = {
    Event((tags ++ resolveTags(notification)).toSet, notification, OffsetDateTime.now(), notification.getClass.getCanonicalName)
  }
}
