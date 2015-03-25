package io.vamp.pulse.main

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{PropsSource, Sink, Source}
import akka.util.Timeout
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.vamp.pulse.eventstream.driver.{Driver, KafkaDriver, SseDriver}
import io.vamp.pulse.eventstream.message.ElasticEvent
import io.vamp.pulse.eventstream.producer.{KafkaMetricsPublisher, SSEMetricsPublisher}
import io.vamp.pulse.storage.client.ESApi
import io.vamp.pulse.storage.dao.ElasticEventDAO
import io.vamp.pulse.storage.engine.ESLocalServer
import org.json4s._
import org.json4s.native.Serialization
import org.slf4j.LoggerFactory
import spray.can.Http

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object Startup extends App {
  private implicit val system = ActorSystem("pulse-system")
  private implicit val mat = ActorFlowMaterializer()
  private implicit val executionContext = system.dispatcher

  private val config = ConfigFactory.load()
  private val logger = Logger(LoggerFactory.getLogger("Main"))
  private implicit val formats = Serialization.formats(NoTypeHints)

  private val streamDriverType = Try(config.getString("stream.driver")).getOrElse("sse")

  private val esConf = config.getConfig("storage.es")
  private var esServer: Option[ESLocalServer] = Option.empty[ESLocalServer]
  private val esClusterName = esConf.getString("cluster.name")

  private var startTriggered = false

  private var stopTriggered = false

  def start: Unit = {
    if(!startTriggered) {
      startTriggered = true
      startup
    } else {
      logger.error("Pulse is already started")
    }
  }

  private def startup: Unit = {

    implicit var esClient: ElasticClient = esConf.getBoolean("embedded.enabled") match {
      case true =>
        logger.info("Starting embedded ES cluster")
        esServer = Option(new ESLocalServer(esClusterName, esConf.getBoolean("embedded.http"), esConf.getBoolean("embedded.local")))
        ElasticClient.fromClient(esServer.get.start.client())

      case false => logger.debug("No embedded ES cluster")
        ESApi.getClient(esClusterName, esConf.getString("host"), esConf.getInt("port"))
    }

    val eventDao = new ElasticEventDAO

    eventDao.createIndex

    val (metricManagerSource: PropsSource[ElasticEvent], driver: Driver) = initSourceAndDriver

    val materializedMap = metricManagerSource.groupedWithin(1000, 1 millis)
      .map { eventList => eventDao.batchInsert(eventList); eventList }
      .to(Sink.ignore).run()

    driver.start(materializedMap.get(metricManagerSource), system)

    httpListen(eventDao)
  }

  start


  def stop: Unit = {
    if(!stopTriggered){
      stopTriggered = true
      stopPulse
    } else {
      logger.error("Pulse is already being stopped")
    }
  }

  private def stopPulse: Unit = {

    if(esServer.isDefined){
      esServer.get.stop
    }

    system.shutdown()
  }


















  private def httpListen(eventDao: ElasticEventDAO) = {

    val server = system.actorOf(HttpActor.props(eventDao), "http-actor")
    val interface = config.getString("http.interface")
    val port = config.getInt("http.port")

    implicit val timeout = Timeout(config.getInt("http.response.timeout") seconds)

    IO(Http)(system) ? Http.Bind(server, interface, port)
  }

  private def initSourceAndDriver = streamDriverType match {
    case "sse" =>
      (Source[ElasticEvent](SSEMetricsPublisher.props), SseDriver)

    case "kafka" =>
      (Source[ElasticEvent](KafkaMetricsPublisher.props), KafkaDriver)

    case _ => throw new Exception(s"Driver $streamDriverType not found")
  }


}
