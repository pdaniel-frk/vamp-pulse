package io.magnetic.vamp.pulse.eventstream.notification

import io.magnetic.vamp_common.notification.Notification

case class UnableToConnect(url: String) extends Notification
case class NotStream(url: String) extends Notification
case class ConnectionSuccessful(url: String) extends Notification