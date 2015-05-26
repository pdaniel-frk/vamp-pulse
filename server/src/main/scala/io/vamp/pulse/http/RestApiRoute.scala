package io.vamp.pulse.http

import akka.pattern.ask
import akka.util.Timeout
import io.vamp.common.akka.CommonActorSupport
import io.vamp.common.http.RestApiBase
import io.vamp.pulse.elasticsearch.ElasticsearchActor
import io.vamp.pulse.model.{Event, EventQuery}
import io.vamp.pulse.notification.PulseNotificationProvider
import org.json4s.Formats
import spray.http.MediaTypes._
import spray.http.StatusCodes.{Created, OK}
import spray.httpx.Json4sSupport

import scala.language.{existentials, postfixOps}

trait RestApiRoute extends RestApiBase with InfoRoute with Json4sSupport with CommonActorSupport with PulseNotificationProvider {

  implicit def timeout: Timeout

  override implicit def json4sFormats: Formats = PulseSerializationFormat.deserializer

  val route = noCachingAllowed {
    allowXhrFromOtherHosts {
      pathPrefix("api" / "v1") {
        accept(`application/json`, `application/x-yaml`) {
          infoRoute ~
            path("events" / "get") {
              pathEndOrSingleSlash {
                post {
                  entity(as[EventQuery]) {
                    query =>
                      onSuccess(actorFor(ElasticsearchActor) ? ElasticsearchActor.Search(query)) {
                        response =>
                          respondWithStatus(OK) {
                            complete(response)
                          }
                      }
                  }
                }
              }
            } ~
            path("events") {
              pathEndOrSingleSlash {
                post {
                  entity(as[Event]) {
                    event =>
                      onSuccess(actorFor(ElasticsearchActor) ? ElasticsearchActor.Index(event)) { response =>
                        respondWithStatus(Created) {
                          complete(response)
                        }
                      }
                  }
                }
              }
            }
        }
      }
    }
  }
}

