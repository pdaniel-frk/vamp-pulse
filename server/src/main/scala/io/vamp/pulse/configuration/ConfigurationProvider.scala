package io.vamp.pulse.configuration

import com.typesafe.config.ConfigFactory

trait ConfigurationProvider {
  protected val confPath: String
  protected lazy val config = ConfigFactory.load().getConfig(confPath)
}

trait DefaultConfigurationProvider extends ConfigurationProvider {
  override protected val confPath: String = ""
}