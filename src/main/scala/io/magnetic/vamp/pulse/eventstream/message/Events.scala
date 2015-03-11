package io.magnetic.vamp.pulse.eventstream.message

import java.time.OffsetDateTime

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration

import scala.util.Try

object EventType extends Enumeration {
  type EventType = Value
  val Numeric, Custom = Value
}
import io.magnetic.vamp.pulse.eventstream.message.EventType._

class EventTypeRef extends TypeReference[EventType.type]



final case class Metric(tags: Seq[String], value: Double, timestamp: OffsetDateTime = OffsetDateTime.now())

final case class ElasticEvent(tags: Seq[String], value: AnyRef, timestamp: OffsetDateTime, properties: EventProperties){
  def convertOutput = {
    this match {
      case ElasticEvent(_, v: Map[_, _], _, props)
        if props.`type` == EventType.Numeric =>
          Try(Metric(tags, v.asInstanceOf[Map[String, Double]]("value"), timestamp)).getOrElse(Metric(tags, 0D, timestamp))

      case ElasticEvent(_, _, _, props) if props.`type` == EventType.Custom => Event(tags, value, timestamp)

      // Figure out what to do in this case from a user perspective. Should we filter these out, throw InternalServerError or else
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