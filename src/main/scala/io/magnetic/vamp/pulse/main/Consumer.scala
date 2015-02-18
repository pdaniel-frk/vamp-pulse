package io.magnetic.vamp.pulse.main

import java.io.InputStream
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import javax.ws.rs.core.{MultivaluedMap, MediaType}
import javax.ws.rs.ext.MessageBodyReader

import akka.actor.ActorSystem
import akka.stream.{ConnectionException, FlowMaterializer}
import akka.stream.scaladsl.{PropsSource, Sink, Source}
import akka.util.Timeout
import com.sclasen.akka.kafka.AkkaConsumerProps
import com.typesafe.config.{ConfigException, ConfigFactory}
import io.magnetic.vamp.pulse.eventstream.driver.{KafkaDriver, SseDriver}
import io.magnetic.vamp.pulse.eventstream.producer._
import io.magnetic.vamp.pulse.storage.engine.{MetricDAO, ESLocalServer}
import kafka.serializer.{StringDecoder, DefaultDecoder}
import org.glassfish.jersey.client.{JerseyClient, JerseyClientBuilder}
import org.glassfish.jersey.media.sse.{EventInput, SseFeature}
import ESLocalServer._

import scala.concurrent.duration._
import scala.util.Try

object Consumer {

  val config = ConfigFactory.load()



  def main(args: Array[String]) {
    ESLocalServer.start
    val dao = new MetricDAO
    ESLocalServer.createAndWaitForIndex(dao.createIndex)

    val driverType = Try(config.getString("stream.driver")).getOrElse("sse")
    
    implicit val system = ActorSystem("metric-consumer")
    implicit val mat = FlowMaterializer()
    
    var metricManagerSource: PropsSource[Metric] = null
    
    lazy val materializedMap = metricManagerSource
      .to(Sink.foreach(elem => dao.insert(elem.payload)))
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
