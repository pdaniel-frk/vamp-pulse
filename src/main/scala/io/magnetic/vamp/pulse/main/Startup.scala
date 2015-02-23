package io.magnetic.vamp.pulse.main

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{PropsSource, Sink, Source}
import com.typesafe.config.ConfigFactory
import io.magnetic.vamp.pulse.eventstream.driver.{KafkaDriver, SseDriver}
import io.magnetic.vamp.pulse.eventstream.producer._
import io.magnetic.vamp.pulse.storage.engine.{ESLocalServer, MetricDAO}
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.write
import akka.io.IO
import spray.can.Http

import scala.util.Try

object Startup {

  val logging = Logging

  val config = ConfigFactory.load()
  implicit val formats = Serialization.formats(NoTypeHints)


  def main(args: Array[String]) {

    val esConf = config.getConfig("storage.es")
    var esServer: Option[ESLocalServer] = Option.empty[ESLocalServer]

    esConf.getBoolean("embedded") match {
      case true  =>  esServer = Option(new ESLocalServer(esConf.getString("cluster.name"))); esServer.get.start
      case false =>  println("")
    }

    // ESLocalServer.start
    val dao = new MetricDAO
    // ESLocalServer.createAndWaitForIndex(dao.createIndex)

    val driverType = Try(config.getString("stream.driver")).getOrElse("sse")
    
    implicit val system = ActorSystem("metric-consumer")
    implicit val mat = ActorFlowMaterializer()
    
    var metricManagerSource: PropsSource[Metric] = null
    
    lazy val materializedMap = metricManagerSource
      .to(Sink.foreach(elem => dao.insert(write(elem))))
      .run()
    
    driverType match {
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

      case _ => println(s"Driver $driverType not found "); system.shutdown()


    }

    

  }


}
