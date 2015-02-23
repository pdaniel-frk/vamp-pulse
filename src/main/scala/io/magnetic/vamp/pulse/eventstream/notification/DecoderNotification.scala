package io.magnetic.vamp.pulse.eventstream.notification

import io.magnetic.vamp_common.notification.Notification

case class UnableToDecode(exception: Exception) extends Notification

