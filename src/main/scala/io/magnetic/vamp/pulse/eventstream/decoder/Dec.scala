package io.magnetic.vamp.pulse.eventstream.decoder
import io.magnetic.vamp.pulse.eventstream.producer.Event
/**
 * Created by lazycoder on 05/03/15.
 */
trait Dec[A <: Event] {
  def fromString(string: String): A
}
