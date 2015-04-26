package io.vamp.pulse.api

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import io.vamp.pulse.api.AggregatorType.AggregatorType

case class EventQuery(tags: List[String] = List.empty, time: TimeRange = TimeRange(), aggregator: Option[Aggregator] = Option.empty, `type`: String = "")
case class TimeRange(from: OffsetDateTime = OffsetDateTime.now().minus(100, ChronoUnit.MINUTES), to: OffsetDateTime = OffsetDateTime.now())
case class Aggregator(`type`: AggregatorType, field: String = "numeric")
final case class Event(tags: Seq[String], value: AnyRef, timestamp: OffsetDateTime = OffsetDateTime.now(), `type`: String = "")

object AggregatorType extends Enumeration {
  type AggregatorType = Value
  val min, max, average, count = Value
}