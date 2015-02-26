package io.magnetic.vamp.pulse.api

import java.time.OffsetDateTime
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.util.Date

case class MetricQuery(tags: List[String] = List.empty, time: TimeRange = TimeRange())
case class TimeRange(from: OffsetDateTime = OffsetDateTime.now().minus(100, ChronoUnit.MINUTES), to: OffsetDateTime = OffsetDateTime.now())