package io.vamp.pulse.eventstream

import io.vamp.pulse.http.PulseSerializationFormat
import io.vamp.pulse.model.Event
import io.vamp.pulse.notification.{PulseNotificationProvider, UnableToDecodeError}
import kafka.serializer.{Decoder, StringDecoder}
import kafka.utils.VerifiableProperties
import org.json4s._
import org.json4s.native.Serialization.read


class EventDecoder(props: VerifiableProperties = null) extends Decoder[Event] with PulseNotificationProvider {

  implicit val formats = PulseSerializationFormat.default

  private val stringDecoder = new StringDecoder(props)

  override def fromBytes(bytes: Array[Byte]): Event = fromString(stringDecoder.fromBytes(bytes))

  def fromString(string: String): Event = {
    try
      read[Event](string)
    catch {
      case ex: MappingException => throwException(UnableToDecodeError(ex))
    }
  }
}