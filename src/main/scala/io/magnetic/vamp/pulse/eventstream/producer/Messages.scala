package io.magnetic.vamp.pulse.eventstream.producer

import java.time.OffsetDateTime

trait Event {
}

final case class Metric(tags: Seq[String], value: Double, timestamp: OffsetDateTime) extends Event

final case class ConcreteEvent(tags: Seq[AnyRef], value: AnyRef, timestamp: OffsetDateTime = OffsetDateTime.now()) extends Event
