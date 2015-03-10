package io.magnetic.vamp.pulse.util

import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.{Instant, OffsetDateTime, ZoneId}

import io.magnetic.vamp.pulse.eventstream.producer.EventType
import io.magnetic.vamp.pulse.eventstream.producer.EventType.EventType
import io.magnetic.vamp.pulse.eventstream.producer.EventType.EventType
import org.json4s.JsonAST.{JNull, JString}
import org.json4s._

class NoneJNullSerializer extends CustomSerializer[Option[_]](format => ({ case JNull => None }, { case None => JNull }))
class EnumerationSerializer extends CustomSerializer[EventType](format => ({ case JString(s) => EventType.withName(s) }, { case value: EventType.Value => JString(value.toString) }))

class OffsetDateTimeSerializer extends CustomSerializer[OffsetDateTime](
   format => (
     {
       case JString(str) => OffsetDateTime.parse(str)
       case JDouble(num) =>
         OffsetDateTime.from(Instant.ofEpochSecond(num.toLong).atZone(ZoneId.of("UTC")))

     },
     { case date: OffsetDateTime => JString(date.format(ISO_OFFSET_DATE_TIME))}
     )
)

object Serializers {
  val formats = DefaultFormats + new OffsetDateTimeSerializer() + new EnumerationSerializer()
}
