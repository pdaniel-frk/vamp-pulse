package io.magnetic.vamp.pulse.api

import io.magnetic.vamp.pulse.eventstream.message.{ElasticEvent, Event, Metric}
import io.magnetic.vamp.pulse.eventstream.message.Event
import io.magnetic.vamp.pulse.storage.engine.{AggregationResult, ResultList, ElasticEventDAO}
import io.magnetic.vamp.pulse.util.Serializers
import org.json4s._
import spray.http.CacheDirectives.`no-store`
import spray.http.HttpHeaders.{RawHeader, `Cache-Control`}
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.Directives._
import spray.routing.Route
import ElasticEvent._

import scala.concurrent.ExecutionContext

class Routes(val metricDao: ElasticEventDAO)(implicit val executionContext: ExecutionContext) extends Json4sSupport {

  protected def jsonResponse = respondWithMediaType(`application/json`) | respondWithHeaders(`Cache-Control`(`no-store`), RawHeader("Pragma", "no-cache"))

  override implicit def json4sFormats: Formats =  Serializers.formats

  def route: Route = jsonResponse {
    pathPrefix("api" / "v1") {
      path("events" / "get") {
        pathEndOrSingleSlash {
          post {
            entity(as[EventQuery]) {
              request =>
                onSuccess(metricDao.getEvents(request)){
                case ResultList(list) => complete(OK, list.map(_.convertOutput))
                case AggregationResult(map) => complete(OK, map)
                case _ => complete(BadRequest)
              }
            }
          }
        }
      }  ~
      path("events") {
        pathEndOrSingleSlash {
          post {
            entity(as[Metric]) {
              request => onSuccess(metricDao.insert(request)){
                case resp => complete(Created, request)
              }
            }
          } ~
          post {
              entity(as[Event]) {
                request => onSuccess(metricDao.insert(request)){
                  case resp => complete(Created, request)
                }
              }
          }
        }

      }
    }
  }

}
