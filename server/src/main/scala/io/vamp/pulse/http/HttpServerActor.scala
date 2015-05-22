package io.vamp.pulse.http

import akka.actor.Props
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.ActorDescription
import io.vamp.common.http.HttpServerBaseActor
import org.json4s.Formats

import scala.concurrent.duration._

object HttpServerActor extends ActorDescription {

  lazy val timeout = Timeout(ConfigFactory.load().getInt("vamp.pulse.rest-api.response-timeout").seconds)

  def props(args: Any*): Props = Props[HttpServerActor]
}

class HttpServerActor extends HttpServerBaseActor with RestApiRoute {

  implicit val timeout = HttpServerActor.timeout

  implicit val formats: Formats = PulseSerializationFormat.serializer
}
