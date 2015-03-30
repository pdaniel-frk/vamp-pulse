package io.vamp.pulse.eventstream.decoder

import io.vamp.pulse.api.Event
import io.vamp.pulse.configuration.DefaultNotification
import io.vamp.pulse.configuration.DefaultNotification._
import io.vamp.pulse.eventstream.message.ElasticEvent._
import io.vamp.pulse.eventstream.message.{ElasticEvent, Metric}
import io.vamp.pulse.eventstream.notification.UnableToDecode
import io.vamp.pulse.util.Serializers
import kafka.serializer.{Decoder, StringDecoder}
import kafka.utils.VerifiableProperties
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.Try


class ElasticEventDecoder(props: VerifiableProperties = null) extends Decoder[ElasticEvent]{
  implicit val formats = Serializers.formats
  val stringDecoder = new StringDecoder(props)
  
  override def fromBytes(bytes: Array[Byte]): ElasticEvent = {
    fromString(stringDecoder.fromBytes(bytes))
  }
  
  def fromString(string: String): ElasticEvent = {
    try
      Try(parse(string).extract[Metric]: ElasticEvent).getOrElse(parse(string).extract[Event])
    catch {
      case ex: MappingException => error(UnableToDecode(ex))
    }

  }
}