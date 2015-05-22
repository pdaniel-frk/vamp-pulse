package io.vamp.pulse

import akka.actor._
import io.vamp.common.akka.{ActorBootstrap, ActorSupport, Bootstrap}
import io.vamp.pulse.elastic.ElasticSearchActor
import io.vamp.pulse.http.RestApiBootstrap

import scala.language.{implicitConversions, postfixOps}

object VampPulse extends App {

  implicit val actorSystem = ActorSystem("vamp-pulse")

  val actorBootstrap = new ActorBootstrap {
    def actors = ActorSupport.actorOf(ElasticSearchActor) :: Nil
  }

  val bootstrap: List[Bootstrap] = actorBootstrap :: RestApiBootstrap :: Nil

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run() = {
      bootstrap.foreach(_.shutdown)
    }
  })

  bootstrap.foreach(_.run)
}
