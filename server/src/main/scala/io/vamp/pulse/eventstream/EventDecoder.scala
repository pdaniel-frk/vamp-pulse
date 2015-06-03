package io.vamp.pulse.eventstream

import io.vamp.pulse.http.PulseSerializationFormat
import io.vamp.pulse.model.Event
import io.vamp.pulse.notification.{PulseNotificationProvider, UnableToDecodeError}
import kafka.serializer.{Decoder, StringDecoder}
import kafka.utils.VerifiableProperties
import org.json4s._
import org.json4s.native.JsonMethods._


class EventDecoder(props: VerifiableProperties = null) extends Decoder[Event] with PulseNotificationProvider {
  implicit val formats = PulseSerializationFormat.api
  val stringDecoder = new StringDecoder(props)

  override def fromBytes(bytes: Array[Byte]): Event = fromString(stringDecoder.fromBytes(bytes))

  def fromString(string: String): Event = {
    try
      parse(string).extract[Event]
    catch {
      case ex: MappingException => error(UnableToDecodeError(ex))
    }
  }
}