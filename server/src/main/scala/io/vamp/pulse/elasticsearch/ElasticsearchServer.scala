package io.vamp.pulse.elasticsearch

import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder._
import org.slf4j.LoggerFactory

class ElasticsearchServer(configuration: Config) {

  private val logger = Logger(LoggerFactory.getLogger(classOf[ElasticsearchServer]))

  private lazy val embeddedNode: Option[Node] = if (configuration.getString("type").toLowerCase == "embedded") {
    Some(nodeBuilder().settings(ImmutableSettings.settingsBuilder()
      .put("transport.tcp.port", configuration.getInt("tcp-port"))
      .put("path.data", configuration.getString("embedded.data-directory"))
      .put("cluster.name", configuration.getString("cluster-name"))
      .put("http.enabled", true).put("node.local", false)
      .build).build)
  } else None

  def start(): Unit = embeddedNode match {
    case Some(node) ⇒
      logger.info("Starting embedded Elasticsearch server.")
      node.start()
    case None ⇒
      logger.info("Remote non-embedded Elasticsearch server will be used.")
  }

  def shutdown(): Unit = embeddedNode.foreach { node ⇒
    logger.info("Shutting down embedded Elasticsearch server.")
    node.close()
  }

  lazy val client = {
    val settings = ImmutableSettings.settingsBuilder().put("cluster.name", configuration.getString("cluster-name")).build
    ElasticClient.remote(settings, configuration.getString("host"), configuration.getInt("tcp-port"))
  }
}

