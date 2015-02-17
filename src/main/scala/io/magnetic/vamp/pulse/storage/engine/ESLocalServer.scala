package io.magnetic.vamp.pulse.storage.engine

import java.nio.file.Files
import org.apache.commons.io.FileUtils
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._

import scala.util.Try


/**
 * Created by lazycoder on 16/02/15.
 */
object ESLocalServer {
  private val clusterName = "vamp-pulse"
  private val dataDir = Files.createTempDirectory("es_data_").toFile
  private val settings = ImmutableSettings.settingsBuilder()
                        .put("path.data", dataDir.toString)
                        .put("cluster.name", clusterName)
                        .build
  
  private lazy val node = nodeBuilder().settings(settings).build
  
  implicit lazy val client = node.client
  
  def start = node.start

  def stop = {
    node.close
    try {
      FileUtils.forceDelete(dataDir)
    } catch {
      case t: Throwable => println(s"Failed to remove temporary ES directory: ${t.getMessage}")
    }
  }


  def createAndWaitForIndex(createIndexRequest: CreateIndexRequest): Unit = {
    client.admin.indices.create(createIndexRequest).actionGet()
    createIndexRequest.indices().foreach((index) => client.admin.cluster.prepareHealth().setWaitForActiveShards(1).execute.actionGet())
  }
  
    

  

}
