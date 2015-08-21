package io.vamp.pulse.http

import akka.actor.{ActorContext, ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.{ActorDescription, ActorSupport, Bootstrap}
import io.vamp.common.http.HttpServerBaseActor
import org.json4s.Formats
import spray.can.Http

import scala.concurrent.duration._

object RestApiBootstrap extends Bootstrap {

  def run(implicit actorSystem: ActorSystem) = {

    val config = ConfigFactory.load().getConfig("vamp.pulse.rest-api")
    val interface = config.getString("interface")
    val port = config.getInt("port")

    val server = ActorSupport.actorOf(HttpServerActor)

    implicit val timeout = HttpServerActor.timeout

    IO(Http)(actorSystem) ? Http.Bind(server, interface, port)
  }
}

object HttpServerActor extends ActorDescription {

  lazy val timeout = Timeout(ConfigFactory.load().getInt("vamp.pulse.rest-api.response-timeout").seconds)

  def props(args: Any*): Props = Props[HttpServerActor]
}

class HttpServerActor extends HttpServerBaseActor with RestApiRoute {

  def actorContext: ActorContext = context

  override def actorRefFactory = super[HttpServerBaseActor].actorRefFactory

  implicit val timeout = HttpServerActor.timeout

  implicit val formats: Formats = PulseSerializationFormat.http
}
