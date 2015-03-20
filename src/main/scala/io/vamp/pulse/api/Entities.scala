package io.vamp.pulse.api

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

case class EventQuery(tags: List[String] = List.empty, time: TimeRange = TimeRange(), aggregator: Option[Aggregator] = Option.empty, `type`: String = "")
case class TimeRange(from: OffsetDateTime = OffsetDateTime.now().minus(100, ChronoUnit.MINUTES), to: OffsetDateTime = OffsetDateTime.now())
case class Aggregator(`type`: String, field: String = "numeric")