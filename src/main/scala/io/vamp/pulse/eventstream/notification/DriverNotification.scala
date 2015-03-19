package io.vamp.pulse.eventstream.notification

import io.vamp.common.notification.Notification

case class UnableToConnect(url: String) extends Notification
case class NotStream(url: String) extends Notification
case class ConnectionSuccessful(url: String) extends Notification