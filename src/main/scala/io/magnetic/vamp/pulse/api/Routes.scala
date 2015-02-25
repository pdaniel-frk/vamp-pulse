package io.magnetic.vamp.pulse.api

import java.time.OffsetDateTime
import java.util.Date

import io.magnetic.vamp.pulse.storage.engine.MetricDAO
import org.json4s.JsonAST.JNull
import org.json4s._
import spray.http.CacheDirectives.`no-store`
import spray.http.HttpHeaders.{RawHeader, `Cache-Control`}
import spray.http.MediaTypes._
import spray.httpx.Json4sSupport
import spray.routing.Route
import spray.http.CacheDirectives.`no-store`
import spray.http.HttpHeaders.{RawHeader, `Cache-Control`}
import spray.http.MediaTypes._
import spray.routing.Directives._
import org.json4s.native.Serialization
import io.magnetic.vamp.pulse.util.Serializers

import scala.concurrent.ExecutionContext
import spray.http.StatusCodes._

class Routes(val executionContext: ExecutionContext, val metricDao: MetricDAO) extends Json4sSupport {

  protected def jsonResponse = respondWithMediaType(`application/json`) | respondWithHeaders(`Cache-Control`(`no-store`), RawHeader("Pragma", "no-cache"))

  override implicit def json4sFormats: Formats =  Serializers.formats

  def route: Route = jsonResponse {
    pathPrefix("api" / "v1") {
      pathPrefix("metrics") {
        pathEndOrSingleSlash {
          post {
            entity(as[MetricQuery]) {
              request => {
                complete(OK, request)
              }
            }
          }
        }
      }
    }
  }

}
