package io.vamp.pulse.elasticsearch

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.config.Config
import io.vamp.pulse.model.Event

trait IndexName {

  def indexConfiguration: Config

  lazy val defaultIndex = indexConfiguration.getString("name")

  def indexNameFor(event: Event): (String, String) = {
    val schema = event.`type`

    val time = {
      val path = s"time-format.$schema"
      val format = indexConfiguration.getString(if (indexConfiguration.hasPath(path)) path else "event")
      OffsetDateTime.now().format(DateTimeFormatter.ofPattern(format))
    }

    s"$defaultIndex-$schema-$time" -> schema
  }
}
