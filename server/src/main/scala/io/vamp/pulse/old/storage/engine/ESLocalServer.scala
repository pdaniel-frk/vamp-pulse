package io.vamp.pulse.old.storage.engine

import java.nio.file.Files

import com.typesafe.scalalogging.Logger
import org.apache.commons.io.FileUtils
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._
import org.slf4j.LoggerFactory


/**
 * Created by lazycoder on 16/02/15.
 */
class ESLocalServer(clusterName: String, httpEnabled: Boolean = false, local: Boolean = false) {
  private val logger = Logger(LoggerFactory.getLogger(classOf[ESLocalServer]))
  private val dataDir = Files.createTempDirectory("es_data_").toFile
  private val settings = ImmutableSettings.settingsBuilder()
                        .put("path.data", dataDir.toString)
                        .put("cluster.name", clusterName)
                        .put("http.enabled", httpEnabled)
                        .put("node.local", local)
                        .build
  
  private lazy val node = nodeBuilder().settings(settings).build
  

  def start = {
    logger.info(s"Starting up local ElasticSearch server, clusterName: $clusterName")
    node.start
  }

  def stop = {
    logger.info(s"Shutting down local ElasticSearch server, clusterName: $clusterName")

    node.close

    try {
      FileUtils.forceDelete(dataDir)
    } catch {
      case t: Throwable => logger.error(s"Failed to remove temporary ES directory: ${t.getMessage}")
    }
  }
}
