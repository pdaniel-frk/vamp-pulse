package io.vamp.pulse.model

import java.time.OffsetDateTime


object Event {

  private val tagDelimiter = ':'

  def expandTags: (Event => Event) = { (event: Event) => event.copy(tags = expandTags(event.tags)) }

  private def expandTags(tags: Set[String]): Set[String] = tags.flatMap { tag =>
    tag.indexOf(tagDelimiter) match {
      case -1 => tag :: Nil
      case index => tag.substring(0, index) :: tag :: Nil
    }
  }
}

case class Event(tags: Set[String], value: AnyRef, timestamp: OffsetDateTime = OffsetDateTime.now(), `type`: Option[String] = None)
