package io.vamp.pulse.eventstream.notification

import io.vamp.common.notification.{ErrorNotification, Notification}


object EmptyEventError extends Notification

case class UnableToDecode(exception: Exception) extends Notification

case class MappingErrorNotification(override val reason: Any, `type`: String) extends ErrorNotification

case class UnableToConnect(url: String) extends Notification

case class NotStream(url: String) extends Notification

case class ConnectionSuccessful(url: String) extends Notification