package io.magnetic.vamp.pulse.api

import java.time.OffsetDateTime
import java.util.Date

case class MetricQuery(tags: List[String] = List.empty, time: TimeRange = TimeRange())
case class TimeRange(from: OffsetDateTime = OffsetDateTime.MIN, to: OffsetDateTime = OffsetDateTime.now())