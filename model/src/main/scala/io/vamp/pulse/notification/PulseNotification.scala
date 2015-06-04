package io.vamp.pulse.notification

import java.time.OffsetDateTime

import io.vamp.common.notification._
import io.vamp.pulse.model.Event

trait PulseEvent {
  def tags: Set[String] = Set()

  def `type`: Option[String] = None

  def value: AnyRef
}

trait PulseNotificationEventFormatter {
  def formatNotification(notification: Notification, tags: List[String] = List.empty): Event
}

trait DefaultNotificationEventFormatter extends PulseNotificationEventFormatter with TagResolverProvider {
  override def formatNotification(notification: Notification, tags: List[String]): Event = {
    Event((tags ++ resolveTags(notification)).toSet, notification, OffsetDateTime.now(), notification.getClass.getCanonicalName)
  }
}
