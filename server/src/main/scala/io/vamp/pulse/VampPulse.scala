package io.vamp.pulse

import akka.actor._
import io.vamp.common.akka.{ActorBootstrap, Bootstrap, IoC}
import io.vamp.pulse.elasticsearch.{ElasticsearchActor, ElasticsearchInitializationActor}
import io.vamp.pulse.eventstream.EventStreamActor
import io.vamp.pulse.http.RestApiBootstrap

import scala.language.{implicitConversions, postfixOps}

trait VampPulse extends App {

  implicit val actorSystem = ActorSystem("vamp-pulse")

  val actorBootstrap = new ActorBootstrap {
    val actors = IoC.createActor(ElasticsearchInitializationActor) ::
      IoC.createActor(ElasticsearchActor) ::
      IoC.createActor(EventStreamActor) :: Nil
  }

  val bootstrap: List[Bootstrap] = actorBootstrap :: RestApiBootstrap :: Nil

  sys.addShutdownHook {
    bootstrap.foreach(_.shutdown)
    actorSystem.shutdown()
  }

  bootstrap.foreach(_.run)
}
