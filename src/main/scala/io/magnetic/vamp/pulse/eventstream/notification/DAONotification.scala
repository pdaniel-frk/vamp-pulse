package io.magnetic.vamp.pulse.eventstream.notification

import io.magnetic.vamp_common.notification.{ErrorNotification, Notification}

case class MappingErrorNotification(override val reason: Any, `type`: String) extends ErrorNotification