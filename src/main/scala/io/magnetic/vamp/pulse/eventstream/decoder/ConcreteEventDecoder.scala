package io.magnetic.vamp.pulse.eventstream.decoder

import java.time.OffsetDateTime

import io.magnetic.vamp.pulse.eventstream.notification.UnableToDecode
import io.magnetic.vamp.pulse.util.Serializers
import kafka.serializer.Decoder
import kafka.serializer.StringDecoder
import io.magnetic.vamp.pulse.eventstream.producer.{ConcreteEvent, Metric}
import kafka.utils.VerifiableProperties
import org.json4s._
import org.json4s.native.JsonMethods._
import io.magnetic.vamp.pulse.configuration.DefaultNotification._


import scala.util.Try

/**
 * Created by lazycoder on 19/02/15.
 */
class ConcreteEventDecoder(props: VerifiableProperties = null) extends Decoder[ConcreteEvent] with Dec[ConcreteEvent]{
  implicit val formats = Serializers.formats
  val stringDecoder = new StringDecoder(props)

  override def fromBytes(bytes: Array[Byte]): ConcreteEvent = {
    fromString(stringDecoder.fromBytes(bytes))
  }

  def fromString(string: String): ConcreteEvent = {
    try {
      parse(string).extract[ConcreteEvent]
    } catch {
      case ex: MappingException => error(UnableToDecode(ex))
    }
  }
}
