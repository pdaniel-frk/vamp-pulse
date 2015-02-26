package io.magnetic.vamp.pulse.main

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{PropsSource, Sink, Source}
import com.typesafe.config.ConfigFactory
import io.magnetic.vamp.pulse.eventstream.driver.{KafkaDriver, SseDriver}
import io.magnetic.vamp.pulse.eventstream.producer._
import io.magnetic.vamp.pulse.storage.client.ESApi
import io.magnetic.vamp.pulse.storage.engine.{ESLocalServer, MetricDAO}
import org.json4s._
import org.json4s.native.Serialization
import spray.can.Http
import scala.concurrent.duration._
import akka.pattern.ask

import org.slf4j.LoggerFactory

import scala.util.Try

private object Startup extends App {
  val config = ConfigFactory.load()
  private val logger = Logger(LoggerFactory.getLogger("Main"))
  implicit val formats = Serialization.formats(NoTypeHints)

  implicit val system = ActorSystem("pulse-system")
  implicit val mat = ActorFlowMaterializer()
  implicit val executionContext = system.dispatcher

  val streamDriverType = Try(config.getString("stream.driver")).getOrElse("sse")

  val esConf = config.getConfig("storage.es")
  var esServer: Option[ESLocalServer] = Option.empty[ESLocalServer]
  val esClusterName = esConf.getString("cluster.name")

  var metricManagerSource: PropsSource[Metric] = null


  esConf.getBoolean("embedded.enabled") match {
    case true =>
      logger.info("Starting embedded ES cluster")
      esServer = Option(new ESLocalServer(esClusterName, esConf.getBoolean("embedded.http")))
      esServer.get.start
    case false => logger.debug("No embedded ES cluster")
  }

  implicit val esClient = ESApi.getClient(esClusterName, esConf.getString("host"), esConf.getInt("port"))

  val metricDao = new MetricDAO

  metricDao.createIndex

  initDriver

  lazy val materializedMap = metricManagerSource
    .to(Sink.foreach(elem =>  {
    try {
      metricDao.insert(elem)
    } catch {
      case ex: Throwable => logger.error("Unable to write metric to ElasticSearch", ex)
    }
  })).run()


  httpListen











  def httpListen = {

    val server = system.actorOf(HttpActor.props(metricDao), "http-actor")
    val interface = config.getString("http.interface")
    val port = config.getInt("http.port")

    implicit val timeout = Timeout(config.getInt("http.response.timeout") seconds)

    IO(Http)(system) ? Http.Bind(server, interface, port)
  }

  def initDriver = streamDriverType match {
    case "sse" =>
      val conf = Map("url" -> config.getString("stream.url"))
      metricManagerSource = Source[Metric](SSEMetricsPublisher.props)
      val src = materializedMap.get(metricManagerSource)
      SseDriver.start(conf, src, system)

    case "kafka" =>
      val conf = Map(
        "url" -> config.getString("stream.url"),
        "topic" -> config.getString("stream.topic"),
        "group" -> config.getString("stream.group"),
        "num" -> config.getString("stream.num")
      )

      metricManagerSource = Source[Metric](KafkaMetricsPublisher.props)
      val src = materializedMap.get(metricManagerSource)

      KafkaDriver.start(conf, src, system)

    case _ => logger.error(s"Driver $streamDriverType not found "); system.shutdown()


  }

}
