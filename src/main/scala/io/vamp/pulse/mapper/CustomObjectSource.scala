package io.vamp.pulse.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.sksamuel.elastic4s.source.DocumentSource
import io.vamp.pulse.util.Serializers

import org.json4s.native.Serialization.{read, write}
import org.json4s._
import Serializers._

class CustomObjectSource(any: Any) extends DocumentSource {
  override def json: String =

  // TODO: Use only json4s module in the whole project
  //
  // Json4s serialisation in order to be able to get rid of jackson
  // Unfortunately lacks implementation of JSR-310 compatibility
  //
  // write(Extraction.decompose(any))
    CustomObjectSource.mapper.writeValueAsString(any)
}

object CustomObjectSource {
  val mapper = new ObjectMapper
  mapper.findAndRegisterModules()
  mapper.registerModule(DefaultScalaModule)
  def apply(any: Any) = new CustomObjectSource(any)

  implicit def anyToObjectSource(any: Any) = apply(any)
}
