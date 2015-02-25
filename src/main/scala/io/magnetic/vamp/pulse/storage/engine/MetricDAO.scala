package io.magnetic.vamp.pulse.storage.engine

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.ObjectSource
import io.magnetic.vamp.pulse.api.MetricQuery
import io.magnetic.vamp.pulse.eventstream.producer.Metric
import io.magnetic.vamp.pulse.mapper.CustomObjectSource


class MetricDAO(implicit client: ElasticClient) {
  private val entity = "metric"
  private val ind = "metrics"

  def insert(metric: Metric) = {
    client.execute {
      index into s"$ind/$entity" doc CustomObjectSource(metric)
    } await
  }

  def getMetrics(metricQuery: MetricQuery) = {
    search in ind->entity term ("tags", metricQuery.tags)
  }
  
  def createIndex = client.execute {
      create index ind mappings (
          entity as (
            "tags" typed StringType,
            "timestamp" typed DateType,
            "value" typed DoubleType
          )
      )
    }
}
