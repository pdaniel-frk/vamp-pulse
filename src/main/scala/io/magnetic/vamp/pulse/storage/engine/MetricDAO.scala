package io.magnetic.vamp.pulse.storage.engine

import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.client.Client

class MetricDAO(implicit client: Client) {
  private val entity = "metric"
  private val index = "metrics"
  
  val mapping =
    """{  
       "properties" : {
                  "name" : {"type" : "string", "index" : "not_analyzed"},
                  "timestamp" : {"type" : "date"},
                  "value" : {"type" : "integer"}
          }
      }""".stripMargin

  def insert(payload: String) = {
    client.prepareIndex(index, entity).setSource(payload).execute().actionGet()
  }
  
  def createIndex = {
    client.admin.indices.prepareCreate(index).addMapping(entity, mapping).request()
  }
}
