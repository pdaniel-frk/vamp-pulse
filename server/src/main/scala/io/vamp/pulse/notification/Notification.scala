package io.vamp.pulse.notification

import io.vamp.common.notification.{ErrorNotification, Notification}


object EmptyEventError extends Notification

object EventIndexError extends Notification

object EventQueryError extends Notification

object EventQueryTimeError extends Notification

case class UnableToDecodeError(exception: Exception) extends Notification

case class MappingErrorNotification(override val reason: Any, schema: String) extends ErrorNotification

case class UnableToConnectError(url: String) extends Notification

case class NotStreamError(url: String) extends Notification

case class ConnectionSuccessful(url: String) extends Notification

case class NoEventStreamDriver(driver: String) extends Notification

case class AggregatorNotSupported() extends Notification