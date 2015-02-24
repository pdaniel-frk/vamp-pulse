package io.magnetic.vamp.pulse.storage.client

import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.ImmutableSettings

/**
 * Created by lazycoder on 23/02/15.
 */
object ESApi {
  def getClient(clusterName: String, host: String, port: Int) = {
    val settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build()
    ElasticClient.remote(settings, host, port)
  }
}
