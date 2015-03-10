package io.magnetic.vamp.pulse.eventstream.producer

import java.time.OffsetDateTime

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration

object EventType extends Enumeration {
  type EventType = Value
  val Numeric, Custom = Value
}
import EventType._

class EventTypeRef extends TypeReference[EventType.type]



final case class Metric(tags: Seq[String], value: Double, timestamp: OffsetDateTime = OffsetDateTime.now())

final case class ElasticEvent(tags: Seq[String], value: AnyRef, timestamp: OffsetDateTime, properties: EventProperties){
  def convertOutput = {
    this match {
      case ElasticEvent(_, v: Map[String, Double], _, props) if props.`type` == EventType.Numeric => Metric(tags, v("value"), timestamp)
      case ElasticEvent(_, _, _, props) if props.`type` == EventType.Custom => Event(tags, value, timestamp)
      case _ => println("Unable to determine the type of event")
    }
  }
}

final case class EventProperties(@JsonScalaEnumeration(classOf[EventTypeRef])`type`: EventType)



final case class Event(tags: Seq[String], value: AnyRef, timestamp: OffsetDateTime = OffsetDateTime.now())

object ElasticEvent {
  implicit def metricToElasticEvent(metric: Metric): ElasticEvent = {
    ElasticEvent(metric.tags, Map("value" -> metric.value), metric.timestamp, EventProperties(EventType.Numeric))
  }

  implicit def eventToElasticEvent(event: Event): ElasticEvent = {
    ElasticEvent(event.tags, event.value, event.timestamp, EventProperties(EventType.Custom))
  }
}