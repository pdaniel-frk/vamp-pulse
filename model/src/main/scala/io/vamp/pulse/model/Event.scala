package io.vamp.pulse.model

import java.time.OffsetDateTime


case class Event(tags: Set[String], value: AnyRef, timestamp: OffsetDateTime = OffsetDateTime.now(), schema: Option[String] = None)
