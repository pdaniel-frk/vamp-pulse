package io.vamp.pulse.eventstream

import akka.actor._
import io.vamp.common.akka.{ CommonSupportForActors, IoC }
import io.vamp.pulse.notification.PulseNotificationProvider

import scala.concurrent.duration._
import scala.language.postfixOps

class SSESupervisionActor(streamUrl: String) extends CommonSupportForActors with PulseNotificationProvider {

  protected val child = IoC.createActor[SSEConnectionActor](streamUrl)

  protected var ticker: Option[Cancellable] = Option.empty

  protected var isOpen = false

  context.watch(child)

  override def receive: Receive = {
    case OpenConnection ⇒
      isOpen = true
      if (ticker.isEmpty || ticker.get.isCancelled)
        ticker = Option(
          context.system.scheduler.schedule(0 milliseconds, 2000 milliseconds, child, OpenConnection)
        )

    case CloseConnection ⇒
      isOpen = false
      child forward CloseConnection
      if (ticker.isDefined && !ticker.get.isCancelled) ticker.get.cancel()
  }
}

