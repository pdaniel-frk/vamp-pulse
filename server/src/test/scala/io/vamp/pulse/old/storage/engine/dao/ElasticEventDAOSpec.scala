package io.vamp.pulse.old.storage.engine.dao

import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import io.vamp.common.akka.FutureSupport
import io.vamp.pulse.old.eventstream.decoder.EventDecoder
import io.vamp.pulse.model.EventQuery
import io.vamp.pulse.old.storage.dao.{AggregationResult, ElasticEventDAO, ResultList}
import io.vamp.pulse.old.storage.engine.ESLocalServer
import io.vamp.pulse.old.util.Serializers
import org.elasticsearch.node.Node
import org.json4s.native.JsonMethods._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source


class ElasticEventDAOSpec extends FlatSpec with Matchers with BeforeAndAfterAll with FutureSupport {

  implicit val formats = Serializers.formats

  val config = ConfigFactory.load()

  val esConf = config.getConfig("storage.es")
  val serverWrapper = new ESLocalServer(esConf.getString("cluster.name"), true, true)
  var server: Node = _

  implicit var esClient: ElasticClient = _
  lazy val dao = new ElasticEventDAO


  val decoder = new EventDecoder()


  override protected def beforeAll() = {
    server = serverWrapper.start
    esClient = ElasticClient.fromClient(server.client())
    super.beforeAll()
  }

  override protected def afterAll() = {
    serverWrapper.stop
  }

//  "MetricDAO" should "be able to insert about 25000 metrics per second in batches of 1000" in {
//    val str = decoder.fromString(Source.fromURL(getClass.getResource("/metric.json")).mkString)
//    val eventList = for(x <- 1 to 1000) yield str
//    val futures = for(x <- 1 to 100) yield dao.batchInsertFuture(eventList)
//
//    val res = Await.result(Futures.sequence(futures), 4000 millis)
//
//    res shouldBe a[List[_]]
//
//
//  }



  "MetricDAO" should "fetch records from elastic-search by tags and date-range" in {
    val str = Source.fromURL(getClass.getResource("/metricQuery.json")).mkString
    val metricQuery = parse(str).extract[EventQuery]
    val resp = Await.result(dao.getEvents(metricQuery), 10 seconds)

    resp shouldBe a[ResultList]

  }


  "MetricDAO" should "aggregate records from elastic-search by tags and date-range" in {
    val str = Source.fromURL(getClass.getResource("/metricQueryAgg.json")).mkString
    val metricQuery = parse(str).extract[EventQuery]
    val resp = Await.result(dao.getEvents(metricQuery), 10 seconds)
    resp shouldBe a[AggregationResult]

  }



}
