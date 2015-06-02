package io.vamp.pulse.model

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object TimeRange {
  def from(from: Option[OffsetDateTime], to: Option[OffsetDateTime], includeLower: Boolean, includeUpper: Boolean): TimeRange = {
    def convert(time: Option[OffsetDateTime]): Option[String] = time.flatMap(t => Some(t.format(DateTimeFormatter.ISO_INSTANT)))

    val lt = if (to.isDefined && !includeUpper) convert(to) else None
    val lte = if (to.isDefined && includeUpper) convert(to) else None
    val gt = if (from.isDefined && !includeLower) convert(from) else None
    val gte = if (from.isDefined && includeLower) convert(from) else None

    TimeRange(lt, lte, gt, gte)
  }
}

case class TimeRange(lt: Option[String], lte: Option[String], gt: Option[String], gte: Option[String])

case class EventQuery(tags: Set[String], time: Option[TimeRange], aggregator: Option[Aggregator] = None)
