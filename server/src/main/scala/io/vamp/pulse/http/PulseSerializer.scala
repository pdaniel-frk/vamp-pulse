package io.vamp.pulse.http

import io.vamp.common.json.{MapSerializer, OffsetDateTimeSerializer, SerializationFormat, SnakeCaseSerializationFormat}
import io.vamp.pulse.model.Aggregator
import org.json4s._
import org.json4s.ext.EnumNameSerializer

object PulseSerializer {
  val default: Formats = SerializationFormat(OffsetDateTimeSerializer, SnakeCaseSerializationFormat, MapSerializer, new EnumNameSerializer(Aggregator))
}