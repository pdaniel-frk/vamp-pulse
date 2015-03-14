package io.magnetic.vamp.pulse.eventstream.message

import java.time.OffsetDateTime

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import org.json4s.native.Serialization._
import scala.util.control.Exception.allCatch

import scala.util.Try

object EventType extends Enumeration {
  type EventType = Value
  val Numeric, JsonBlob, Typed = Value
}
import io.magnetic.vamp.pulse.eventstream.message.EventType._

class EventTypeRef extends TypeReference[EventType.type]

trait Evt {

}


final case class Metric(tags: Seq[String], value: Double, timestamp: OffsetDateTime = OffsetDateTime.now()) extends Evt


final case class ElasticEvent(tags: Seq[String], value: AnyRef, timestamp: OffsetDateTime, properties: EventProperties){
  def convertOutput = {
    this match {
      case ElasticEvent(_, v: Map[_, _], _, props)
        if props.`type` == EventType.Numeric =>
          Try(Metric(tags, v.asInstanceOf[Map[String, Double]]("value"), timestamp)).getOrElse(Metric(tags, 0D, timestamp))
      case _ => Event(tags, value, timestamp)
    }
  }
}

final case class EventProperties(@JsonScalaEnumeration(classOf[EventTypeRef])`type`: EventType)



final case class Event(tags: Seq[String], value: AnyRef, timestamp: OffsetDateTime = OffsetDateTime.now(), `type`: String = "") extends Evt

object ElasticEvent {
  implicit def metricToElasticEvent(metric: Metric): ElasticEvent = {
    ElasticEvent(metric.tags, Map("numeric" -> metric.value), metric.timestamp, EventProperties(EventType.Numeric))
  }


  implicit def eventToElasticEvent(event: Event): ElasticEvent = {
    if(event.`type`.isEmpty) {
      ElasticEvent(event.tags, Map("blob" -> event.value), event.timestamp, EventProperties(EventType.JsonBlob))
    } else {
      ElasticEvent(event.tags, Map(event.`type` -> event.value), event.timestamp, EventProperties(EventType.Typed))
    }

  }
}