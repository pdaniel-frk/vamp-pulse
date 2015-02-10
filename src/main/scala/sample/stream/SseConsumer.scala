package sample.stream

import java.io.InputStream
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import javax.ws.rs.core.{MultivaluedMap, MediaType}
import javax.ws.rs.ext.MessageBodyReader

import akka.actor.ActorSystem
import akka.stream.{ConnectionException, FlowMaterializer}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import com.typesafe.config.{ConfigException, ConfigFactory}
import io.magnetic.vamp.pulse.eventstream.driver.SseDriver
import io.magnetic.vamp.pulse.eventstream.producer.MetricsManager
import org.glassfish.jersey.client.{JerseyClient, JerseyClientBuilder}
import org.glassfish.jersey.media.sse.{EventInput, SseFeature}

import scala.concurrent.duration._

object SseConsumer {

  val config = ConfigFactory.load()


  def main(args: Array[String]) {
    var driverType = ""
    try {
      driverType = config.getString("stream.driver")
    } catch {
      case _ => throw new RuntimeException("You have to specify stream.driver in a string format")
    }

    implicit val system = ActorSystem("sse-consumer")
    implicit val mat = FlowMaterializer()
    val metricManagerSource = Source[MetricsManager.Metric](MetricsManager.props)
    val materializedMap = metricManagerSource
      .map { elem => println(elem); elem}
      .to(Sink.ignore)
      .run()

    driverType match {
      case "sse" => SseDriver.start(config.getString("stream.url"), materializedMap.get(metricManagerSource))
      case _ => println(s"Driver $driverType not found "); system.shutdown()
    }
  }


}
