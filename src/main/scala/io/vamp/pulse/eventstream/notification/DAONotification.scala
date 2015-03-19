package io.vamp.pulse.eventstream.notification

import io.vamp.common.notification.{ErrorNotification, Notification}

case class MappingErrorNotification(override val reason: Any, `type`: String) extends ErrorNotification