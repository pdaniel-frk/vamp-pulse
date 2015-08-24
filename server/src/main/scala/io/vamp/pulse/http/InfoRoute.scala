package io.vamp.pulse.http

import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.ActorSystemProvider
import io.vamp.common.http.{InfoBaseRoute, InfoMessageBase, RestApiBase}
import io.vamp.common.vitals.JvmVitals
import io.vamp.pulse.elasticsearch.ElasticsearchActor
import io.vamp.pulse.eventstream.EventStreamActor

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.{existentials, postfixOps}

case class InfoMessage(message: String, version: String, jvm: JvmVitals, elasticSearch: Any, stream: Any) extends InfoMessageBase

trait InfoRoute extends InfoBaseRoute {
  this: RestApiBase with ActorSystemProvider =>

  val infoMessage = ConfigFactory.load().getString("vamp.pulse.rest-api.info.message")

  val componentInfoTimeout = Timeout(ConfigFactory.load().getInt("vamp.pulse.rest-api.info.timeout") seconds)

  def info: Future[InfoMessageBase] = retrieve(ElasticsearchActor :: EventStreamActor :: Nil).map { result =>
    InfoMessage(infoMessage,
      getClass.getPackage.getImplementationVersion,
      jvmVitals(),
      result.get(ElasticsearchActor),
      result.get(EventStreamActor)
    )
  }
}
