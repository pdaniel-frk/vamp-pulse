package io.vamp.pulse.http

import io.vamp.common.json.{ MapSerializer, OffsetDateTimeSerializer, SerializationFormat, SnakeCaseSerializationFormat }
import io.vamp.pulse.model.Aggregator
import org.json4s._
import org.json4s.ext.EnumNameSerializer

object PulseSerializationFormat {

  val http: Formats = SerializationFormat(OffsetDateTimeSerializer, SnakeCaseSerializationFormat, MapSerializer, new EnumNameSerializer(Aggregator))

  val default: Formats = SerializationFormat(OffsetDateTimeSerializer, new EnumNameSerializer(Aggregator))
}