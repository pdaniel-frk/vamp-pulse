package io.vamp.pulse.eventstream.notification

import io.vamp.common.notification.Notification

case class UnableToDecode(exception: Exception) extends Notification

