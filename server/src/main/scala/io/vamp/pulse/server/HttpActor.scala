package io.vamp.pulse.server

import akka.actor.{ActorLogging, Props}
import io.vamp.common.notification.NotificationErrorException
import io.vamp.pulse.elastic.ElasticSearchEventDAO
import spray.http.StatusCodes._
import spray.http.{HttpRequest, HttpResponse, Timedout}
import spray.routing._
import spray.util.LoggingContext

object HttpActor {
  def props(eventDao: ElasticSearchEventDAO): Props = Props(new HttpActor(eventDao))
}

class HttpActor(val eventDao: ElasticSearchEventDAO) extends HttpServiceActor with ActorLogging {

  def exceptionHandler = ExceptionHandler {
    case e: NotificationErrorException => respondWithStatus(BadRequest) {
      complete(e.message)
    }
    case e: Exception => requestUri { uri =>
      log.info(e.getClass.toString)
      log.error(s"Request to {} could not be handled $e", uri)
      complete(InternalServerError)
    }
  }

  def rejectionHandler = RejectionHandler {
    case MalformedRequestContentRejection(msg, Some(e: NotificationErrorException)) :: _ => respondWithStatus(BadRequest) {
      complete("The request content was malformed:\n" + msg)
    }
    case MalformedRequestContentRejection(msg, Some(e: Throwable)) :: _ => complete(BadRequest)
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

