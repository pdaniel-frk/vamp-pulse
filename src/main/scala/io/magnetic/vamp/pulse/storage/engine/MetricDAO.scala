package io.magnetic.vamp.pulse.storage.engine

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.ObjectSource
import io.magnetic.vamp.pulse.eventstream.producer.Metric




class MetricDAO(implicit client: ElasticClient) {
  private val entity = "metric"
  private val ind = "metrics"
  
  val mapping =
    """{  
       "properties" : {
                  "name" : {"type" : "string", "index" : "not_analyzed"},
                  "timestamp" : {"type" : "date"},
                  "value" : {"type" : "integer"}
          }
      }""".stripMargin

  def insert(metric: Metric) = {
    client.execute {
      index into s"$ind/$entity" doc ObjectSource(metric)
    }
  }
  
  def createIndex = client.execute {
      create index ind mappings (
          entity as (
            "name" typed StringType analyzer NotAnalyzed,
            "timestamp" typed DateType,
            "value" typed IntegerType
          )
      )
    }
}
