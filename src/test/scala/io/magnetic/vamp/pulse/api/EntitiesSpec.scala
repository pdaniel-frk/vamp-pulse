package io.magnetic.vamp.pulse.api


import io.magnetic.vamp.pulse.util.Serializers
import org.scalatest.{Matchers, FlatSpec}
import org.json4s.native.JsonMethods._


import scala.io.Source

/**
 * Created by lazycoder on 25/02/15.
 */
class EntitiesSpec extends FlatSpec  with Matchers{
  implicit val formats = Serializers.formats

  "MetricQuery" should "be able to be decoded from json" in {
    val str = Source.fromURL(getClass.getResource("/metricQuery.json")).mkString
    val metricQuery = parse(str).extract[EventQuery]

    metricQuery shouldBe an[EventQuery]
  }
}
