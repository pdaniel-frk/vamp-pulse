package io.vamp.pulse.eventstream.driver

import akka.actor.{DeadLetter, ActorSystem}
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, FlatSpecLike, FlatSpec}

/**
 * Created by lazycoder on 16/03/15.
 */
class SSEConnectionActorSpec(_system: ActorSystem) extends TestKit(_system) with FlatSpecLike with Matchers{
  def this() = this(ActorSystem.create("TestSystem"))
  val conf = ConfigFactory.load()

  val ref = _system.actorOf(SSEConnectionActor.props(conf.getString("stream.url"), testActor))
  val ref2 = _system.actorOf(SSEConnectionActor.props("http://ya.ru/mudak",testActor))



  ref ! OpenConnection
  ref2 ! OpenConnection

  Thread.sleep(2000)
}
