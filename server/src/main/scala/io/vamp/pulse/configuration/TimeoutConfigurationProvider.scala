package io.vamp.pulse.configuration

trait TimeoutConfigurationProvider extends ConfigurationProvider {
  override protected val confPath: String = "vamp.pulse.event-stream.sse.timeout"
}
