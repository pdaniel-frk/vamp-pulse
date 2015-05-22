package io.vamp.pulse

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{PropsSource, Sink, Source}
import akka.util.Timeout
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.vamp.pulse.elastic.{ElasticSearchEventDAO, ElasticSearchLocalServer}
import io.vamp.pulse.eventstream._
import io.vamp.pulse.model.Event
import org.elasticsearch.common.settings.ImmutableSettings
import org.slf4j.LoggerFactory
import spray.can.Http

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

object PulseBootstrap extends App {
  private implicit val system = ActorSystem("pulse-system")
  private implicit val mat = ActorFlowMaterializer()
  private implicit val executionContext = system.dispatcher

  private val config = ConfigFactory.load()
  private val logger = Logger(LoggerFactory.getLogger("Main"))

  private val streamDriverType = Try(config.getString("stream.driver")).getOrElse("sse")

  private val esConf = config.getConfig("storage.es")
  private var esServer: Option[ElasticSearchLocalServer] = Option.empty[ElasticSearchLocalServer]
  private val esClusterName = esConf.getString("cluster.name")

  private var startTriggered = false

  private var stopTriggered = false

  def start(): Unit = {
    if (!startTriggered) {
      startTriggered = true
      startup()
    } else {
      logger.error("Pulse is already started")
    }
  }

  private def startup(): Unit = {

    implicit val esClient: ElasticClient = esConf.getBoolean("embedded.enabled") match {
      case true =>
        logger.info("Starting embedded ES cluster")
        esServer = Option(new ElasticSearchLocalServer(esClusterName, esConf.getBoolean("embedded.http"), esConf.getBoolean("embedded.local")))
        ElasticClient.fromClient(esServer.get.start.client())

      case false => logger.debug("No embedded ES cluster")
        getPulseClient(esClusterName, esConf.getString("host"), esConf.getInt("port"))
    }

    val eventDao = new ElasticSearchEventDAO

    eventDao.createIndex

    val (metricManagerSource: PropsSource[Event], driver: Driver) = initSourceAndDriver

    val materializedMap = metricManagerSource.groupedWithin(1000, 1 millis)
      .map { eventList => eventDao.batchInsert(eventList); eventList }
      .to(Sink.ignore).run()

    driver.start(materializedMap.get(metricManagerSource), system)

    // start HTTP server
  }

  private def getPulseClient(clusterName: String, host: String, port: Int) = {
    val settings = ImmutableSettings.settingsBuilder()
      .put("cluster.name", clusterName).build()

    ElasticClient.remote(settings, host, port)
  }

  start()

  def stop(): Unit = {
    if (!stopTriggered) {
      stopTriggered = true
      stopPulse()
    } else {
      logger.error("Pulse is already being stopped")
    }
  }

  private def stopPulse(): Unit = {

    if (esServer.isDefined) {
      esServer.get.stop()
    }

    system.shutdown()
  }

  private def initSourceAndDriver = streamDriverType match {
    case "sse" =>
      (Source[Event](SSEMetricsPublisher.props), SseDriver)

    case "kafka" =>
      (Source[Event](KafkaMetricsPublisher.props), KafkaDriver)

    case _ => throw new Exception(s"Driver $streamDriverType not found")
  }
}
