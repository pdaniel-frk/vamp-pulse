package io.vamp.pulse.old.eventstream.driver

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Ignore, Matchers, WordSpecLike}

@Ignore class SSEDriverSpec(_system: ActorSystem) extends TestKit(_system)
with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter with ImplicitSender {

  def this() = this(ActorSystem("Drivers"))

  val config = ConfigFactory.load()


  override protected def beforeAll() = {
    SseDriver.start(self, _system)
  }

  override protected def afterAll() = {
    TestKit.shutdownActorSystem(_system)
    SseDriver.stop()

  }

  //  "SSE Driver" must {
  //    "fetch message from a stream and send it to consumer" in {
  //      expectMsgClass[Event](classOf[Event])
  //    }
  //  }
}
