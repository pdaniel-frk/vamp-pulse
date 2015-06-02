package io.vamp.pulse.elasticsearch

import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._
import org.slf4j.LoggerFactory

trait ElasticsearchServer {

  def client: ElasticClient

  def start(): Unit = {}

  def shutdown(): Unit = {}
}

class RemoteElasticsearchServer(configuration: Config) extends ElasticsearchServer {

  private val settings = ImmutableSettings.settingsBuilder()
    .put("cluster.name", configuration.getString("cluster-name"))
    .build

  val client = ElasticClient.remote(settings, configuration.getString("host"), configuration.getInt("tcp-port"))
}

class EmbeddedElasticsearchServer(configuration: Config) extends ElasticsearchServer {

  private val logger = Logger(LoggerFactory.getLogger(classOf[EmbeddedElasticsearchServer]))

  private lazy val node = nodeBuilder().settings(ImmutableSettings.settingsBuilder()
    .put("transport.tcp.port", configuration.getInt("tcp-port"))
    .put("path.data", configuration.getString("data-directory"))
    .put("cluster.name", configuration.getString("cluster-name"))
    .put("http.enabled", true).put("node.local", false)
    .build).build

  lazy val client = ElasticClient.remote(ImmutableSettings.settingsBuilder()
    .put("cluster.name", configuration.getString("cluster-name"))
    .build, "localhost", configuration.getInt("tcp-port"))

  override def start() = {
    logger.info("Starting embedded Elasticsearch server")
    node.start()
  }

  override def shutdown() = {
    logger.info("Shutting down embedded Elasticsearch server")
    node.close()
  }
}
