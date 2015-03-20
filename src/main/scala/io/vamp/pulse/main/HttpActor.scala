package io.vamp.pulse.main

import akka.actor.{ActorLogging, Props}
import io.vamp.common.notification.NotificationErrorException
import io.vamp.pulse.api.Routes
import io.vamp.pulse.storage.engine.ElasticEventDAO
import spray.http.StatusCodes._
import spray.http.{HttpRequest, HttpResponse, Timedout}
import spray.routing._
import spray.util.LoggingContext

class HttpActor(val eventDao: ElasticEventDAO) extends HttpServiceActor with ActorLogging {

  def exceptionHandler = ExceptionHandler {
    case e: NotificationErrorException => complete(BadRequest, e.message)
    case e: Exception => requestUri { uri =>
      log.info(e.getClass.toString)
      log.error(s"Request to {} could not be handled $e", uri)
      complete(InternalServerError)
    }
  }

  def rejectionHandler = RejectionHandler {
    case MalformedRequestContentRejection(msg, Some(e: NotificationErrorException)) :: _ => complete(BadRequest, "The request content was malformed:\n" + msg)
    case MalformedRequestContentRejection(msg, Some(e: Throwable)) :: _ => println(e.getClass); complete(BadRequest)
  }

  def routingSettings = RoutingSettings.default

  def loggingContext = LoggingContext.fromActorRefFactory

  def handleTimeouts: Receive = {
    case Timedout(x: HttpRequest) =>
      sender() ! HttpResponse(InternalServerError)
  }

  val executionContext = actorRefFactory.dispatcher

  def receive = handleTimeouts orElse runRoute(new Routes(eventDao)(executionContext).route)(exceptionHandler, rejectionHandler, context, routingSettings, loggingContext)
}

object HttpActor {
  def props(eventDao: ElasticEventDAO): Props = Props(new HttpActor(eventDao))
}