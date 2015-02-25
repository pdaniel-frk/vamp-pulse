package io.magnetic.vamp.pulse.util

import java.time.OffsetDateTime

import org.json4s._
import org.json4s.JsonAST.{JString, JNull}
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

class NoneJNullSerializer extends CustomSerializer[Option[_]](format => ({ case JNull => None }, { case None => JNull }))
class OffsetDateTimeSerializer extends CustomSerializer[OffsetDateTime](format => ({case JString(str) => OffsetDateTime.parse(str)}, { case date: OffsetDateTime => JString(date.format(ISO_OFFSET_DATE_TIME))}))

object Serializers {
  val formats = DefaultFormats + new OffsetDateTimeSerializer()
}
