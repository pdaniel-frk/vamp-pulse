package io.vamp.pulse.eventstream

import akka.actor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


class SSESupervisionActor(streamUrl: String, producerRef: ActorRef) extends Actor with ActorLogging {

  protected val child = context.actorOf(SSEConnectionActor.props(streamUrl, producerRef))

  protected var ticker: Option[Cancellable] = Option.empty

  protected var isOpen = false

  context.watch(child)

  override def receive: Receive = {
    case OpenConnection => isOpen = true
      if (ticker.isEmpty || ticker.get.isCancelled)
        ticker = Option(
          context.system.scheduler.schedule(0 milliseconds, 2000 milliseconds, child, OpenConnection)
        )

    case CloseConnection => isOpen = false
      child forward CloseConnection
      if (ticker.isDefined && !ticker.get.isCancelled) ticker.get.cancel()
  }
}

object SSESupervisionActor {
  def props(streamUrl: String, producerRef: ActorRef): Props = Props(new SSESupervisionActor(streamUrl, producerRef))
}
