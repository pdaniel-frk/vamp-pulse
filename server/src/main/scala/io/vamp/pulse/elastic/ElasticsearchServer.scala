package io.vamp.pulse.elastic

import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._
import org.slf4j.LoggerFactory

trait ElasticsearchServer {

  def elasticClient: ElasticClient

  def start(): Unit = {}

  def shutdown(): Unit = {}
}

class EmbeddedElasticsearchServer(configuration: Config) extends ElasticsearchServer {

  private val logger = Logger(LoggerFactory.getLogger(classOf[EmbeddedElasticsearchServer]))

  private val settings = ImmutableSettings.settingsBuilder()
    .put("path.data", configuration.getString("data-directory"))
    .put("cluster.name", configuration.getString("cluster-name"))
    .put("http.enabled", configuration.getBoolean("rest-api"))
    .put("node.local", configuration.getBoolean("local-only"))
    .build

  private lazy val node = nodeBuilder().settings(settings).build

  override def start() = {
    logger.info("Starting embedded Elasticsearch server")
    node.start()
  }

  override def shutdown() = {
    logger.info("Shutting down embedded Elasticsearch server")
    node.close()
  }

  def elasticClient = ElasticClient.fromNode(node)
}

class RemoteElasticsearchServer(configuration: Config) extends ElasticsearchServer {

  private val settings = ImmutableSettings.settingsBuilder()
    .put("cluster.name", configuration.getString("cluster-name"))
    .build

  def elasticClient = ElasticClient.remote(settings, configuration.getString("host"), configuration.getInt("port"))
}
