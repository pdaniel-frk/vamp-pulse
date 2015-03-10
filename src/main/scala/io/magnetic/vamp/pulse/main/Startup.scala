package io.magnetic.vamp.pulse.main

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{PropsSource, Sink, Source}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.magnetic.vamp.pulse.eventstream.driver.{KafkaDriver, SseDriver}
import io.magnetic.vamp.pulse.eventstream.message.ElasticEvent
import io.magnetic.vamp.pulse.eventstream.producer._
import io.magnetic.vamp.pulse.storage.client.ESApi
import io.magnetic.vamp.pulse.storage.engine.{ESLocalServer, ElasticEventDAO}
import org.json4s._
import org.json4s.native.Serialization
import org.slf4j.LoggerFactory
import spray.can.Http

import scala.concurrent.duration._
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

  var metricManagerSource: PropsSource[ElasticEvent] = null


  esConf.getBoolean("embedded.enabled") match {
    case true =>
      logger.info("Starting embedded ES cluster")
      esServer = Option(new ESLocalServer(esClusterName, esConf.getBoolean("embedded.http")))
      esServer.get.start
    case false => logger.debug("No embedded ES cluster")
  }

  implicit val esClient = ESApi.getClient(esClusterName, esConf.getString("host"), esConf.getInt("port"))

  val metricDao = new ElasticEventDAO

  metricDao.createIndex

  initDriver

  lazy val materializedMap = metricManagerSource
    .to(Sink.foreach(elem =>  {
      metricDao.insert(elem)
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
      metricManagerSource = Source[ElasticEvent](SSEMetricsPublisher.props)
      SseDriver.start(materializedMap.get(metricManagerSource), system)

    case "kafka" =>
      metricManagerSource = Source[ElasticEvent](KafkaMetricsPublisher.props)
      KafkaDriver.start(materializedMap.get(metricManagerSource), system)

    case _ => logger.error(s"Driver $streamDriverType not found "); system.shutdown()


  }

}
