package io.magnetic.vamp.pulse.eventstream.driver

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import io.magnetic.vamp.pulse.eventstream.message.Event
import org.scalatest.{BeforeAndAfterAll, WordSpecLike, BeforeAndAfter}
import org.scalatest.Matchers
import scala.concurrent.duration._


class KafkaDriverSpec(_system: ActorSystem) extends TestKit(_system)
with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter with ImplicitSender {

  def this() = this(ActorSystem("Drivers"))

  val config = ConfigFactory.load()

  override protected def afterAll(): Registration = {
    TestKit.shutdownActorSystem(_system)
  }

  "Kafka Driver" must {
    "fetch message from a stream and send it to consumer" in {
      val conf = Map(
        "url" -> config.getString("stream.kafka_url"),
        "topic" -> config.getString("stream.topic"),
        "group" -> config.getString("stream.group"),
        "num" -> config.getString("stream.num")
      )
      val producer = new KafkaProducer(conf("topic"), config.getString("stream.kafka_broker"))

      KafkaDriver.start(conf, self, _system)
      producer.send("test")
      expectMsg("test")
      KafkaDriver.stop()
    }
  }

}
