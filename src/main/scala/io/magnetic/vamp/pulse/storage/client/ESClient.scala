package io.magnetic.vamp.pulse.storage.client

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest

/**
 * Created by lazycoder on 23/02/15.
 */
class ESClient {
  implicit lazy val client =


  def createAndWaitForIndex(createIndexRequest: CreateIndexRequest): Unit = {
    client.admin.indices.create(createIndexRequest).actionGet()
    createIndexRequest.indices().foreach((index) => client.admin.cluster.prepareHealth().setWaitForActiveShards(1).execute.actionGet())
  }


}
