package io.vamp.pulse.eventstream.driver

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatest.Ignore

@Ignore class KafkaDriverSpec(_system: ActorSystem) extends TestKit(_system)
with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter with ImplicitSender {

  def this() = this(ActorSystem("Drivers"))

  val config = ConfigFactory.load()

  override protected def afterAll(): Registration = {
    TestKit.shutdownActorSystem(_system)
  }

  "Kafka Driver" must {
    "fetch message from a stream and send it to consumer" in {
      val producer = new KafkaProducer(config.getString("stream.topic"), config.getString("stream.kafka_broker"))

      KafkaDriver.start(self, _system)
      producer.send("test")
      expectMsg("test")
      KafkaDriver.stop()
    }
  }

}
