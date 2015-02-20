package io.magnetic.vamp.pulse.eventstream.driver

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import io.magnetic.vamp.pulse.eventstream.producer.Metric
import org.scalatest.{BeforeAndAfterAll, WordSpecLike, BeforeAndAfter}
import scala.concurrent.duration._
import org.scalatest.Matchers


class SSEDriverSpec(_system: ActorSystem) extends TestKit(_system)
                        with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter with ImplicitSender{
  
  def this() = this(ActorSystem("Drivers"))
  val config = ConfigFactory.load()
  
  override protected def afterAll(): Registration = {
    TestKit.shutdownActorSystem(_system)
  }
  

  "SSE Driver" must {
    "fetch message from a stream and send it to consumer" in {
      val conf = Map(
        "url" -> config.getString("stream.sse_url")
      )
      SseDriver.start(conf, self, _system)
      expectMsgClass[Metric](classOf[Metric])
      SseDriver.stop()
    }
    
  }

  override protected def before(fun: => Any): Registration = {

  }
}
