package io.magnetic.vamp.pulse.eventstream.decoder

import java.time.{OffsetDateTime, Instant, LocalDateTime, LocalDate}

import io.magnetic.vamp.pulse.eventstream.message.ElasticEvent
import org.json4s.native.Serialization.{ read, write, writePretty }


import org.scalatest.{WordSpec, FlatSpec, WordSpecLike}
import org.scalatest.Matchers
import io.magnetic.vamp.pulse.eventstream.producer.Event
import scala.io.Source

class EventDecoderSpec extends FlatSpec  with Matchers {
  "MetricDecoder" should "be able to decode valid metric json into an Event" in {

    val metricDecoder = new ElasticEventDecoder()
    val str = Source.fromURL(getClass.getResource("/metric.json")).mkString
    val metric = metricDecoder.fromString(str)

    metric shouldBe an[ElasticEvent]
  }

  "MetricDecoder" should "be able to decode valid event json into an Event" in {

    val metricDecoder = new ElasticEventDecoder()
    val str = Source.fromURL(getClass.getResource("/event.json")).mkString
    val metric = metricDecoder.fromString(str)

    metric shouldBe an[ElasticEvent]
  }
}
