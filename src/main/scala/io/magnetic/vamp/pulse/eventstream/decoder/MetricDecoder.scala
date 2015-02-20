package io.magnetic.vamp.pulse.eventstream.decoder
import kafka.serializer.Decoder
import kafka.serializer.StringDecoder
import io.magnetic.vamp.pulse.eventstream.producer.Metric
import kafka.utils.VerifiableProperties
import org.json4s._
import org.json4s.native.JsonMethods._
/**
 * Created by lazycoder on 19/02/15.
 */
class MetricDecoder(props: VerifiableProperties = null) extends Decoder[Metric]{
  implicit val formats = DefaultFormats
  val stringDecoder = new StringDecoder(props)
  
  override def fromBytes(bytes: Array[Byte]): Metric = {
    fromString(stringDecoder.fromBytes(bytes))
  }
  
  def fromString(string: String): Metric = {
    parse(string).extract[Metric]
  }
}
