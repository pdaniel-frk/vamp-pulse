package io.vamp.pulse.http

import java.util.concurrent.TimeUnit

import akka.actor.ActorContext
import akka.util.Timeout
import io.vamp.common.akka.{ActorDescription, ActorRefFactoryExecutionContextProvider, ActorSupport}
import org.json4s.Formats
import org.specs2.mutable.Specification
import spray.http.HttpHeaders.Accept
import spray.http.StatusCodes._
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest

import scala.language.postfixOps

class RestApiRouteSpec extends Specification with Specs2RouteTest with HttpService with RestApiRoute with ActorSupport with ActorRefFactoryExecutionContextProvider {

  args(skipAll = true)



  override def actorRefFactory = system

  val formats: Formats = PulseSerializationFormat.default

  implicit val timeout = Timeout(3, TimeUnit.SECONDS)

  "The service" should {

    "return an info message" in {
      Get("/api/v1/info").withHeaders(Accept(`application/x-yaml`)) ~> route ~> check {
        responseAs[String] must contain("Hi")
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> route ~> check {
        handled must beFalse
      }
    }

    "return a NotFound error for PUT requests to the root path" in {
      Put() ~> sealRoute(route) ~> check {
        status === NotFound
        responseAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }

  def actorContext: ActorContext = ???

  override def actorFor(actorDescription: ActorDescription) = ???
}
