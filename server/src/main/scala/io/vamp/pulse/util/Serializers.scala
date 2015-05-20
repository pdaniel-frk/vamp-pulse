package io.vamp.pulse.util

import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.{Instant, OffsetDateTime, ZoneId}

import io.vamp.pulse.model.Aggregator
import org.json4s.JsonAST.{JNull, JString}
import org.json4s._
import org.json4s.ext.EnumNameSerializer

class NoneJNullSerializer extends CustomSerializer[Option[_]](format => ( {
  case JNull => None
}, {
  case None => JNull
}))

class OffsetDateTimeSerializer extends CustomSerializer[OffsetDateTime](
  format => ( {
    case JString(str) => OffsetDateTime.parse(str)
    case JDouble(num) =>
      OffsetDateTime.from(Instant.ofEpochSecond(num.toLong).atZone(ZoneId.of("UTC")))

  }, {
    case date: OffsetDateTime => JString(date.format(ISO_OFFSET_DATE_TIME))
  }
    )
)

object Serializers {
  implicit val formats = DefaultFormats + new OffsetDateTimeSerializer() + new EnumNameSerializer(Aggregator)
}
