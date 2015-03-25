package io.vamp.pulse.eventstream.driver

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import io.vamp.pulse.eventstream.message.ElasticEvent
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}

import org.scalatest.Ignore

@Ignore class SSEDriverSpec(_system: ActorSystem) extends TestKit(_system)
                        with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter with ImplicitSender{
  
  def this() = this(ActorSystem("Drivers"))
  val config = ConfigFactory.load()


  override protected def beforeAll(): Registration = {
    SseDriver.start(self, _system)
  }

  override protected def afterAll(): Registration = {
    TestKit.shutdownActorSystem(_system)
    SseDriver.stop()

  }
  

  "SSE Driver" must {
    "fetch message from a stream and send it to consumer" in {
      expectMsgClass[ElasticEvent](classOf[ElasticEvent])
    }
  }

  override protected def before(fun: => Any): Registration = {

  }
}
