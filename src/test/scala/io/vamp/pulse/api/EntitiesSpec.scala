package io.vamp.pulse.api

import io.vamp.pulse.eventstream.decoder.ElasticEventDecoder
import io.vamp.pulse.eventstream.message.{ElasticEvent, Metric}
import io.vamp.pulse.util.Serializers
import org.json4s.native.JsonMethods._
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

/**
 * Created by lazycoder on 25/02/15.
 */
class EntitiesSpec extends FlatSpec  with Matchers {
  implicit val formats = Serializers.formats
  val decoder = new ElasticEventDecoder()

  "MetricQuery" should "be able to be decoded from json" in {
    val str = Source.fromURL(getClass.getResource("/metricQuery.json")).mkString
    val eventQuery = parse(str).extract[EventQuery]

    eventQuery shouldBe an[EventQuery]
  }

  "ElasticEvent" should "be able to be decoded from metric json" in {
    val str = Source.fromURL(getClass.getResource("/metric.json")).mkString
    val elasticEvent = decoder.fromString(str)

    elasticEvent shouldBe an[ElasticEvent]
  }

  "ElasticEvent" should "be able to be decoded from event json" in {
    val str = Source.fromURL(getClass.getResource("/event.json")).mkString
    val elasticEvent = decoder.fromString(str)

    elasticEvent shouldBe an[ElasticEvent]
  }

  "Metric" should "be able to be produced by ElasticEvent decoded from Metric json" in {
    val str = Source.fromURL(getClass.getResource("/metric.json")).mkString
    val elasticEvent = decoder.fromString(str)

    elasticEvent.convertOutput shouldBe a[Metric]
  }

  "Event" should "be able to be produced by ElasticEvent decoded from Event json" in {
    val str = Source.fromURL(getClass.getResource("/event.json")).mkString
    val elasticEvent = decoder.fromString(str)

    elasticEvent.convertOutput shouldBe an[Event]
  }
}
