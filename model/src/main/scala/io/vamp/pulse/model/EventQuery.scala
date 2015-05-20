package io.vamp.pulse.model

import java.time.OffsetDateTime


case class TimeRange(from: Option[OffsetDateTime], to: Option[OffsetDateTime])

case class EventQuery(tags: Set[String], time: Option[TimeRange], aggregator: Option[Aggregator] = None)
