package io.vamp.pulse.util

import io.vamp.pulse.model.Aggregator
import org.json4s._
import org.json4s.ext.EnumNameSerializer

object PulseSerializer {
  val default = DefaultFormats + new OffsetDateTimeSerializer() + new EnumNameSerializer(Aggregator)
}
