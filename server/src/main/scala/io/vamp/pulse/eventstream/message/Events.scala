package io.vamp.pulse.eventstream.message

import java.time.OffsetDateTime

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import io.vamp.pulse.model.Event
import io.vamp.pulse.util.Serializers
import org.json4s.ext.EnumNameSerializer

import scala.language.{implicitConversions, postfixOps}
import scala.util.Try

object EventType extends Enumeration {
  type EventType = Value
  val Numeric, JsonBlob, Typed = Value
}

import io.vamp.pulse.eventstream.message.EventType._

class EventTypeRef extends TypeReference[EventType.type]


final case class Metric(tags: Set[String], value: Double, timestamp: OffsetDateTime = OffsetDateTime.now())


final case class ElasticEvent(tags: Set[String], value: AnyRef, timestamp: OffsetDateTime, properties: EventProperties, blob: AnyRef = "") {
  def convertOutput = {
    this match {
      case ElasticEvent(_, v: Map[_, _], _, props, _)
        if props.`type` == EventType.Numeric =>
        Try(Metric(tags, v.asInstanceOf[Map[String, Double]]("numeric"), timestamp)).getOrElse(Metric(tags, 0D, timestamp))

      case ElasticEvent(_, _, _, props, b) if props.`type` == EventType.JsonBlob => Event(tags, b, timestamp)

      case ElasticEvent(_, v: Map[_, _], _, props, _) => Event(tags, v.asInstanceOf[Map[String, AnyRef]].getOrElse(props.objectType, ""), timestamp, Some(props.objectType))
    }
  }
}

final case class EventProperties(@JsonScalaEnumeration(classOf[EventTypeRef]) `type`: EventType, objectType: String = "")


object ElasticEvent {
  implicit val formats = Serializers.formats + new EnumNameSerializer(EventType)

  private val tagDelimiter = ':'

  def expandTags: (Set[String] => Set[String]) = { (tags: Set[String]) =>
    tags.flatMap { tag =>
      tag.indexOf(tagDelimiter) match {
        case -1 => tag :: Nil
        case index => tag.substring(0, index) :: tag :: Nil
      }
    }
  }

  implicit def metricToElasticEvent(metric: Metric): ElasticEvent = {
    ElasticEvent(expandTags(metric.tags), Map("numeric" -> metric.value), metric.timestamp, EventProperties(EventType.Numeric))
  }


  implicit def eventToElasticEvent(event: Event): ElasticEvent = {
    if (event.schema.isEmpty) {
      ElasticEvent(expandTags(event.tags), Map.empty, event.timestamp, EventProperties(EventType.JsonBlob), event.value)
    } else {
      ElasticEvent(expandTags(event.tags), Map(event.schema.get -> event.value), event.timestamp, EventProperties(EventType.Typed, event.schema.get))
    }

  }
}