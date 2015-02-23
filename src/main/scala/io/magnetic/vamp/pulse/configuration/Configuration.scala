package io.magnetic.vamp.pulse.configuration

import io.magnetic.vamp_common.notification.{DefaultPackageMessageResolverProvider, NotificationProvider, MessageResolverProvider, LoggingNotificationProvider}

object DefaultNotification extends LoggingNotificationProvider with DefaultPackageMessageResolverProvider {

}