package io.magnetic.vamp.pulse.eventstream.decoder

import io.magnetic.vamp.pulse.configuration.DefaultNotification._
import io.magnetic.vamp.pulse.eventstream.notification.UnableToDecode
import io.magnetic.vamp.pulse.eventstream.producer.{Event, Metric}
import io.magnetic.vamp.pulse.util.Serializers
import kafka.serializer.{Decoder, StringDecoder}
import kafka.utils.VerifiableProperties
import org.json4s._
import org.json4s.native.JsonMethods._
import Metric._

import scala.util.Try

/**
 * Created by lazycoder on 19/02/15.
 */
class EventDecoder(props: VerifiableProperties = null) extends Decoder[Event]{
  implicit val formats = Serializers.formats
  val stringDecoder = new StringDecoder(props)
  
  override def fromBytes(bytes: Array[Byte]): Event = {
    fromString(stringDecoder.fromBytes(bytes))
  }
  
  def fromString(string: String): Event = {
    try {
      Try(metricToEvent(parse(string).extract[Metric])).getOrElse(parse(string).extract[Event])
    } catch {
      case ex: MappingException => error(UnableToDecode(ex))
    }
  }
}