package io.vamp.pulse.elasticsearch

import com.sksamuel.elastic4s.source.DocumentSource
import io.vamp.pulse.http.PulseSerializationFormat
import org.json4s.native.Serialization._

import scala.language.implicitConversions

class CustomObjectSource(any: AnyRef) extends DocumentSource {
  implicit val formats = PulseSerializationFormat.default

  override def json: String = write(any)
}

object CustomObjectSource {

  def apply(any: AnyRef) = new CustomObjectSource(any)

  implicit def anyToObjectSource(any: AnyRef): CustomObjectSource = apply(any)
}
