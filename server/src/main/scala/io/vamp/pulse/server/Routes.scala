package io.vamp.pulse.server

import akka.util.Timeout
import io.vamp.common.akka.ExecutionContextProvider
import io.vamp.common.vitals.JmxVitalsProvider
import io.vamp.pulse.elastic.{ElasticSearchAggregationResult, ElasticSearchResultList, ElasticSearchEventDAO}
import io.vamp.pulse.model.{Event, EventQuery}
import io.vamp.pulse.util.PulseSerializer
import org.elasticsearch.action.index.IndexResponse
import org.json4s._
import spray.http.CacheDirectives.`no-store`
import spray.http.HttpHeaders.{RawHeader, `Cache-Control`}
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.Directives._
import spray.routing.Route

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class Routes(val eventDao: ElasticSearchEventDAO)(implicit val executionContext: ExecutionContext) extends Json4sSupport with JmxVitalsProvider with ExecutionContextProvider {

  protected def jsonResponse = respondWithMediaType(`application/json`) | respondWithHeaders(`Cache-Control`(`no-store`), RawHeader("Pragma", "no-cache"))

  override implicit def json4sFormats: Formats = PulseSerializer.default

  implicit val timeout = Timeout(5 seconds)

  def route: Route = jsonResponse {
    pathPrefix("api" / "v1") {
      path("info") {
        pathEndOrSingleSlash {
          get {
            onSuccess(vitals) { info =>
              respondWithStatus(OK) {
                complete(info)
              }
            }
          }
        }
      } ~
        path("events" / "reset") {
          pathEndOrSingleSlash {
            get {
              requestEntityEmpty {
                ctx => {
                  eventDao.cleanupEvents
                  ctx.complete(OK)
                }
              }
            }
          }
        } ~
        path("events" / "get") {
          pathEndOrSingleSlash {
            post {
              entity(as[EventQuery]) {
                request =>
                  onSuccess(eventDao.getEvents(request)) {
                    case ElasticSearchResultList(list) =>
                      respondWithStatus(OK) {
                        complete(list)
                      }
                    case ElasticSearchAggregationResult(map) =>
                      respondWithStatus(OK) {
                        complete(map)
                      }
                    case _ => complete(BadRequest)
                  }
              }
            }
          }
        } ~
        path("events") {
          pathEndOrSingleSlash {
            post {
              entity(as[Event]) {
                request => onSuccess(eventDao.insert(request)) {
                  case resp: IndexResponse =>
                    respondWithStatus(Created) {
                      complete(request)
                    }
                }
              }
            }
          }
        }
    }
  }
}
