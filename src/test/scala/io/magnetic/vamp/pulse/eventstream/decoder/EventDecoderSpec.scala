package io.magnetic.vamp.pulse.eventstream.decoder

import io.magnetic.vamp.pulse.eventstream.message.ElasticEvent
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class EventDecoderSpec extends FlatSpec  with Matchers {
  "EventDecoder" should "be able to decode valid metric json into an Event" in {

    val metricDecoder = new ElasticEventDecoder()
    val str = Source.fromURL(getClass.getResource("/metric.json")).mkString
    val metric = metricDecoder.fromString(str)

    metric shouldBe an[ElasticEvent]
  }

  "EventDecoder" should "be able to decode valid event json into an Event" in {

    val metricDecoder = new ElasticEventDecoder()
    val str = Source.fromURL(getClass.getResource("/event.json")).mkString
    val metric = metricDecoder.fromString(str)

    metric shouldBe an[ElasticEvent]
  }
}
