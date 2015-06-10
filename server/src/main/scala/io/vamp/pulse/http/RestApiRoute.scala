package io.vamp.pulse.http

import akka.pattern.ask
import akka.util.Timeout
import io.vamp.common.akka.ActorSupport
import io.vamp.common.http.RestApiBase
import io.vamp.pulse.elasticsearch.ElasticsearchActor
import io.vamp.pulse.elasticsearch.ElasticsearchActor.EventRequestEnvelope
import io.vamp.pulse.model.{Event, EventQuery}
import io.vamp.pulse.notification.PulseNotificationProvider
import org.json4s.Formats
import spray.http.HttpHeaders.{Link, RawHeader}
import spray.http.MediaTypes._
import spray.http.StatusCodes.{Created, OK}
import spray.http.{StatusCode, Uri}
import spray.httpx.Json4sSupport
import spray.routing.Route

import scala.language.{existentials, postfixOps}

trait RestApiRoute extends RestApiBase with InfoRoute with Json4sSupport with PulseNotificationProvider {
  this: ActorSupport =>

  implicit def timeout: Timeout

  override implicit def json4sFormats: Formats = PulseSerializationFormat.default

  def pageAndPerPage(perPage: Int = 30) = parameters(('page.as[Long] ? 1, 'per_page.as[Long] ? perPage))

  def respondWith(status: StatusCode, response: Any): Route = {

    def links(uri: Uri, envelope: OffsetResponseEnvelope[_]) = {

      def link(page: Long, param: Link.Param) = Link.Value(uri.copy(fragment = None, query = ("per_page" -> s"${envelope.perPage}") +: ("page" -> s"$page") +: uri.query), param)
      def first = link(1, Link.first)
      def last = link(envelope.total / envelope.perPage + 1, Link.last)
      def previous = link(if (envelope.page > 1) envelope.page - 1 else 1, Link.prev)
      def next = link(if (envelope.page < envelope.total / envelope.perPage + 1) envelope.page + 1 else envelope.total / envelope.perPage + 1, Link.next)

      Link(first, previous, next, last)
    }

    respondWithStatus(status) {
      response match {
        case envelope: OffsetResponseEnvelope[_] =>
          requestUri { uri =>
            respondWithHeader(links(uri, envelope)) {
              respondWithHeader(RawHeader("X-Total-Count", s"${envelope.total}")) {
                complete(envelope.response)
              }
            }
          }

        case _ => complete(response)
      }
    }
  }

  val route = noCachingAllowed {
    allowXhrFromOtherHosts {
      pathPrefix("api" / "v1") {
        accept(`application/json`, `application/x-yaml`) {
          infoRoute ~
            path("events" / "get") {
              pathEndOrSingleSlash {
                post {
                  pageAndPerPage() { (page, perPage) =>
                    entity(as[EventQuery]) { query =>
                      onSuccess(actorFor(ElasticsearchActor) ? ElasticsearchActor.Search(EventRequestEnvelope(query, page, perPage))) { response =>
                        respondWith(OK, response)
                      }
                    }
                  }
                }
              }
            } ~
            path("events") {
              pathEndOrSingleSlash {
                post {
                  entity(as[Event]) { event =>
                    onSuccess(actorFor(ElasticsearchActor) ? ElasticsearchActor.Index(event)) { response =>
                      respondWith(Created, response)
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

