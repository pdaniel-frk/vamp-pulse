package io.magnetic.vamp.pulse.api

import io.magnetic.vamp.pulse.eventstream.producer.{ConcreteEvent, Metric}
import io.magnetic.vamp.pulse.storage.engine.MetricDAO
import io.magnetic.vamp.pulse.util.Serializers
import org.json4s._
import spray.http.CacheDirectives.`no-store`
import spray.http.HttpHeaders.{RawHeader, `Cache-Control`}
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.Json4sSupport
import spray.routing.Directives._
import spray.routing.Route

import scala.concurrent.ExecutionContext

class Routes(val metricDao: MetricDAO)(implicit val executionContext: ExecutionContext) extends Json4sSupport {

  protected def jsonResponse = respondWithMediaType(`application/json`) | respondWithHeaders(`Cache-Control`(`no-store`), RawHeader("Pragma", "no-cache"))

  override implicit def json4sFormats: Formats =  Serializers.formats

  def route: Route = jsonResponse {
    pathPrefix("api" / "v1") {
      path("metrics/get") {
        pathEndOrSingleSlash {
          post {
            entity(as[MetricQuery]) {
              request =>
                onSuccess(metricDao.getMetrics(request)){
                case resp: List[Metric] => complete(OK, resp)
                case resp: Map[String, Double] => complete(OK, resp)
                case _ => complete(BadRequest)
              }
            }
          }
        }
      } ~
      path("metrics") {
        pathEndOrSingleSlash {
          post {
            entity(as[Metric]) {
              request => onSuccess(metricDao.insert(request)){
                case resp => complete(Created, request)
              }
            }
          } ~
          post {
            entity(as[ConcreteEvent]) {
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
