package io.vamp.pulse.http

import akka.actor.Actor
import akka.util.Timeout
import io.vamp.common.akka.ExecutionContextProvider
import io.vamp.common.http.RestApiBase
import io.vamp.pulse.notification.PulseNotificationProvider
import spray.http.MediaTypes._

import scala.language.{existentials, postfixOps}

trait RestApiRoute extends RestApiBase with InfoRoute with PulseNotificationProvider {
  this: Actor with ExecutionContextProvider =>

  implicit def timeout: Timeout

  val route = noCachingAllowed {
    allowXhrFromOtherHosts {
      pathPrefix("api" / "v1") {
        accept(`application/json`, `application/x-yaml`) {
          infoRoute //~
          //            path("events" / "reset") {
          //              pathEndOrSingleSlash {
          //                get {
          //                  requestEntityEmpty {
          //                    ctx => {
          //                      //eventDao.cleanupEvents
          //                      ctx.complete(OK)
          //                    }
          //                  }
          //                }
          //              }
          //            } ~
          //            path("events" / "get") {
          //              pathEndOrSingleSlash {
          //                post {
          //                  entity(as[EventQuery]) {
          //                    request =>
          //                      onSuccess(eventDao.getEvents(request)) {
          //                        case ElasticSearchResultList(list) =>
          //                          respondWithStatus(OK) {
          //                            complete(list)
          //                          }
          //                        case ElasticSearchAggregationResult(map) =>
          //                          respondWithStatus(OK) {
          //                            complete(map)
          //                          }
          //                        case _ => complete(BadRequest)
          //                      }
          //                  }
          //                }
          //              }
          //            } ~
          //            path("events") {
          //              pathEndOrSingleSlash {
          //                post {
          //                  entity(as[Event]) {
          //                    request =>
          //                      onSuccess(eventDao.insert(request)) {
          //                        case resp: IndexResponse =>
          //                          respondWithStatus(Created) {
          //                            complete(request)
          //                          }
          //                      }
          //                  }
          //                }
          //              }
          //            }
        }
      }
    }
  }
}

