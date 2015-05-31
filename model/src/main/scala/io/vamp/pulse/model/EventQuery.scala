package io.vamp.pulse.model

import java.time.OffsetDateTime


case class TimeRange(from: Option[OffsetDateTime], to: Option[OffsetDateTime], `include-lower`: Boolean = true, `include-upper`: Boolean = true)

case class EventQuery(tags: Set[String], time: Option[TimeRange], aggregator: Option[Aggregator] = None)
