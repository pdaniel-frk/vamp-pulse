package io.magnetic.vamp.pulse.storage.engine

import com.sksamuel.elastic4s.{QueryDefinition, ElasticClient}
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.ObjectSource
import io.magnetic.vamp.pulse.api.MetricQuery
import io.magnetic.vamp.pulse.eventstream.producer.Metric
import io.magnetic.vamp.pulse.mapper.CustomObjectSource
import io.magnetic.vamp.pulse.eventstream.decoder.MetricDecoder

import scala.collection.mutable.Queue
import scala.concurrent.ExecutionContext


class MetricDAO(implicit client: ElasticClient, implicit val executionContext: ExecutionContext) {
  private val entity = "metric"
  private val ind = "metrics"
  private val decoder = new MetricDecoder()

  def insert(metric: Metric) = {
    client.execute {
      index into s"$ind/$entity" doc CustomObjectSource(metric)
    } await
  }

  //TODO: Figure out timestamp issues with elastic: We can only use epoch now + we get epoch as a double from elastic.
  def getMetrics(metricQuery: MetricQuery) = {
    val shouldMatch = metricQuery.tags.length

    val queries: Queue[QueryDefinition] = Queue(rangeQuery("timestamp") from metricQuery.time.from.toEpochSecond to metricQuery.time.to.toEpochSecond)

    if(metricQuery.tags.length > 0) queries += termsQuery("tags", metricQuery.tags:_*) minimumShouldMatch(shouldMatch)

    client.execute {
      search in ind -> entity query {
        must  (
          queries
        )
      }
    } map {
      resp => List(resp.getHits.hits().map((hit) =>  decoder.fromString(hit.sourceAsString())): _*)
    }


  }
  
  def createIndex = client.execute {
      create index ind mappings (
          entity as (
            "tags" typed StringType,
            "timestamp" typed DateType store(true) format("dateOptionalTime"),
            "value" typed DoubleType
          )
      )
    }
}
