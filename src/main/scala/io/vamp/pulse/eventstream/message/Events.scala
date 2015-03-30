package io.vamp.pulse.eventstream.message

import java.time.OffsetDateTime

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import io.vamp.pulse.api.Event
import io.vamp.pulse.util.Serializers
import org.json4s.ext.EnumNameSerializer

import scala.util.Try

object EventType extends Enumeration {
  type EventType = Value
  val Numeric, JsonBlob, Typed = Value
}
import io.vamp.pulse.eventstream.message.EventType._

class EventTypeRef extends TypeReference[EventType.type]


final case class Metric(tags: Seq[String], value: Double, timestamp: OffsetDateTime = OffsetDateTime.now())


final case class ElasticEvent(tags: Seq[String], value: AnyRef, timestamp: OffsetDateTime, properties: EventProperties, blob: AnyRef = ""){
  def convertOutput = {
    this match {
      case ElasticEvent(_, v: Map[_, _], _, props, _)
        if props.`type` == EventType.Numeric =>
          Try(Metric(tags, v.asInstanceOf[Map[String, Double]]("numeric"), timestamp)).getOrElse(Metric(tags, 0D, timestamp))

      case ElasticEvent(_, _, _, props, b) if props.`type` == EventType.JsonBlob => Event(tags, b, timestamp)

      case ElasticEvent(_, v: Map[_, _], _, props, _) => Event(tags, v.asInstanceOf[Map[String, AnyRef]].getOrElse(props.objectType, ""), timestamp, props.objectType)
    }
  }
}

final case class EventProperties(@JsonScalaEnumeration(classOf[EventTypeRef])`type`: EventType, objectType: String = "")





object ElasticEvent {
  implicit val formats = Serializers.formats +  new EnumNameSerializer(EventType)


  implicit def metricToElasticEvent(metric: Metric): ElasticEvent = {
    ElasticEvent(metric.tags, Map("numeric" -> metric.value), metric.timestamp, EventProperties(EventType.Numeric))
  }


  implicit def eventToElasticEvent(event: Event): ElasticEvent = {
    if(event.`type`.isEmpty) {
      ElasticEvent(event.tags, Map.empty, event.timestamp, EventProperties(EventType.JsonBlob), event.value)
    } else {
      ElasticEvent(event.tags, Map(event.`type` -> event.value), event.timestamp, EventProperties(EventType.Typed, event.`type`))
    }

  }
}