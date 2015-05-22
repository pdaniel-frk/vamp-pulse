package io.vamp.pulse.elastic

import akka.actor._
import io.vamp.common.akka.Bootstrap.{Shutdown, Start}
import io.vamp.common.akka._
import io.vamp.common.vitals.InfoRequest
import io.vamp.pulse.notification.PulseNotificationProvider

import scala.language.postfixOps

object ElasticSearchActor extends ActorDescription {

  def props(args: Any*): Props = Props[ElasticSearchActor]

}

class ElasticSearchActor extends CommonActorSupport with PulseNotificationProvider {

  def receive: Receive = {
    case Start =>

    case Shutdown =>

    case InfoRequest => sender ! "You Know, for Search..."
  }
}
