package io.vamp.pulse.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.sksamuel.elastic4s.source.DocumentSource


class CustomObjectSource(any: Any) extends DocumentSource {
  override def json: String = CustomObjectSource.mapper.writeValueAsString(any)
}

object CustomObjectSource {
  val mapper = new ObjectMapper
  mapper.findAndRegisterModules()
  mapper.registerModule(DefaultScalaModule)
  def apply(any: Any) = new CustomObjectSource(any)

  implicit def anyToObjectSource(any: Any): CustomObjectSource = apply(any)
}
