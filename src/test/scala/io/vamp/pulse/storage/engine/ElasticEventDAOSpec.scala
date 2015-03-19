package io.vamp.pulse.storage.engine

import com.typesafe.config.ConfigFactory
import io.vamp.pulse.eventstream.message.Event
import io.vamp.pulse.api.EventQuery
import io.vamp.pulse.eventstream.decoder.ElasticEventDecoder
import io.vamp.pulse.storage.client.ESApi
import io.vamp.pulse.util.Serializers
import org.json4s.native.JsonMethods._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source


class ElasticEventDAOSpec extends FlatSpec with Matchers {
  implicit val formats = Serializers.formats

  val config = ConfigFactory.load()

  val esConf = config.getConfig("storage.es")
  implicit val esClient = ESApi.getClient(esConf.getString("cluster.name"), esConf.getString("host"), esConf.getInt("port"))
  val dao = new ElasticEventDAO


  val decoder = new ElasticEventDecoder()


  "MetricDAO" should "Should fetch records from elastic-search by tags and date-range" in {
    val str = Source.fromURL(getClass.getResource("/metricQuery.json")).mkString
    val metricQuery = parse(str).extract[EventQuery]
    val resp = Await.result(dao.getEvents(metricQuery), 10 seconds)

    resp shouldBe a[ResultList]

  }


  "MetricDAO" should "Should aggregate records from elastic-search by tags and date-range" in {
    val str = Source.fromURL(getClass.getResource("/metricQueryAgg.json")).mkString
    val metricQuery = parse(str).extract[EventQuery]
    val resp = Await.result(dao.getEvents(metricQuery), 10 seconds)
    resp shouldBe a[AggregationResult]

  }



}
