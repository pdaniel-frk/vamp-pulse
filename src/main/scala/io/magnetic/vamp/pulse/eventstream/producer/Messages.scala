package io.magnetic.vamp.pulse.eventstream.producer

import java.time.OffsetDateTime


final case class Metric(tags: Seq[String], value: Double, timestamp: OffsetDateTime = OffsetDateTime.now()) {
  def toEvent = {
  }
}

final case class Event(tags: Seq[String], value: AnyRef, timestamp: OffsetDateTime = OffsetDateTime.now()) {
  def convertOutput = {
    this match {
      case Event(tags, value: Map[String, AnyRef], timestamp) if value.getOrElse(Event.eventTypeField, "none") == "numeric" && value.getOrElse("value", Nil).isInstanceOf[Double] => Metric(tags, value("value").asInstanceOf[Double], timestamp)
      case ev: Event => ev
    }
  }
}

object Event {
  val eventTypeField = "vamp_event_type"
  implicit def metricToEvent(metric: Metric): Event = {
    Event(metric.tags, Map("value" -> metric.value, eventTypeField -> "numeric"), metric.timestamp)
  }
}