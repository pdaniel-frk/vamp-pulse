package io.magnetic.vamp.pulse.main

import akka.actor.{ActorLogging, Props}
import io.magnetic.vamp.pulse.api.Routes
import io.magnetic.vamp.pulse.storage.engine.ElasticEventDAO
import io.magnetic.vamp_common.notification.NotificationErrorException
import spray.http.StatusCodes._
import spray.http.{HttpRequest, HttpResponse, Timedout}
import spray.routing._
import spray.util.LoggingContext

class HttpActor(val metricDAO: ElasticEventDAO) extends HttpServiceActor with ActorLogging {

  def exceptionHandler = ExceptionHandler {
    case e: Exception => requestUri { uri =>
      println(e)
      log.error(s"Request to {} could not be handled $e", uri)
      complete(InternalServerError)
    }
  }

  def rejectionHandler = RejectionHandler {
    case MalformedRequestContentRejection(msg, Some(e: NotificationErrorException)) :: _ => complete(BadRequest, "The request content was malformed:\n" + msg)
    case MalformedRequestContentRejection(msg, _) :: _ =>println(msg); complete(InternalServerError)
  }

  def routingSettings = RoutingSettings.default

  def loggingContext = LoggingContext.fromActorRefFactory

  def handleTimeouts: Receive = {
    case Timedout(x: HttpRequest) =>
      sender() ! HttpResponse(InternalServerError)
  }

  val executionContext = actorRefFactory.dispatcher

  def receive = handleTimeouts orElse runRoute(new Routes(metricDAO)(executionContext).route)(exceptionHandler, rejectionHandler, context, routingSettings, loggingContext)
}

object HttpActor {
  def props(metricDAO: ElasticEventDAO): Props = Props(new HttpActor(metricDAO))
}