package io.magnetic.vamp.pulse.storage.engine

import com.sksamuel.elastic4s.{SearchType, FilterDefinition, QueryDefinition, ElasticClient}
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.ObjectSource
import io.magnetic.vamp.pulse.api.{Aggregator, MetricQuery}
import io.magnetic.vamp.pulse.eventstream.producer.{ConcreteEvent, Metric}
import io.magnetic.vamp.pulse.mapper.CustomObjectSource
import io.magnetic.vamp.pulse.eventstream.decoder.{ConcreteEventDecoder, MetricDecoder}
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation
import org.elasticsearch.search.aggregations.metrics.avg.InternalAvg

import scala.collection.mutable.Queue
import scala.concurrent.{Future, ExecutionContext}


class MetricDAO(implicit client: ElasticClient, implicit val executionContext: ExecutionContext) {
  private val metricEntity = "metric"
  private val metricIndex = "metrics"
  private val eventEntity = "event"

  private val metricDecoder = new MetricDecoder()
  private val eventDecoder = new ConcreteEventDecoder()

  def insert(metric: Metric) = {
    client.execute {
      index into s"$metricIndex/$metricEntity" doc CustomObjectSource(metric)
    }
  }

  def insert(event: ConcreteEvent) = {
    client.execute {
      index into s"$metricIndex/$eventEntity" doc CustomObjectSource(event)
    }
  }

  //TODO: Figure out timestamp issues with elastic: We can only use epoch now + we get epoch as a double from elastic.
  def getMetrics(metricQuery: MetricQuery): Future[Any] = {
    val entity = metricQuery.`type` match {
      case "metric" => metricEntity
      case "event"  => eventEntity
    }

    if(metricQuery.aggregator.isEmpty) {
      getPlainEvents(metricQuery, entity)
    } else {
      if(entity == eventEntity) throw new Exception("Not implemented")
      else aggregateMetrics(metricQuery)
    }
  }

  def getEvents(metricQuery: MetricQuery): Future[Any] = {
    getPlainEvents(metricQuery, eventEntity)
  }



  private def getPlainEvents(metricQuery: MetricQuery, entity: String) = {
    val tagNum = metricQuery.tags.length

    val decoder = entity match {
      case e: String if e == eventEntity => eventDecoder
      case e: String if e == metricEntity => metricDecoder
    }

    val queries: Queue[QueryDefinition] = Queue(
      rangeQuery("timestamp") from metricQuery.time.from.toEpochSecond to metricQuery.time.to.toEpochSecond
    )

    if(tagNum > 0) queries += termsQuery("tags", metricQuery.tags:_*) minimumShouldMatch(tagNum)

    client.execute {
      search in metricIndex -> entity query {
        must  (
          queries
        )
      } start 0 limit 30
    } map {
      resp => List(resp.getHits.hits().map((hit) =>  decoder.fromString(hit.sourceAsString())): _*)
    }
  }

  //TODO: Implement different aggregators here, for now we blindly do average.
  def aggregateMetrics(metricQuery: MetricQuery) = {

    val filters: Queue[FilterDefinition] = Queue(
      rangeFilter("timestamp") from metricQuery.time.from.toEpochSecond to metricQuery.time.to.toEpochSecond
    )

    if(!metricQuery.tags.isEmpty) filters += termsFilter("tags", metricQuery.tags :_*) execution("and")


    client.execute {
      search  in metricIndex -> metricEntity searchType SearchType.Count aggs {
        aggregation filter "filter_agg" filter {
          must(filters)
        } aggs {
          metricQuery.aggregator.getOrElse(Aggregator("average")).`type` match {
            case "average" => aggregation avg "val_agg" field "value"
            case "min" => aggregation min  "val_agg" field "value"
            case "max" => aggregation max  "val_agg" field "value"
            case str: String => throw new Exception(s"No such aggregation implemented $str")
          }
        }
      }
    } map {
      resp => var value: Double = resp.getAggregations
        .get("filter_agg").asInstanceOf[InternalFilter]
        .getAggregations.get("val_agg").asInstanceOf[InternalNumericMetricsAggregation.SingleValue]
        .value()
        //TODO: Wrapper for result types to check corner-cases
        //TODO: Also might be a good idea not to use java api and map response json directly to whatever case classes we might have
        if(value.compareTo(Double.NaN) == 0) value = 0D

        Map("value" -> value)
    }
  }
  
  def createIndex = client.execute {
      create index metricIndex mappings (
          metricEntity as (
            "tags" typed StringType,
            "timestamp" typed DateType,
            "value" typed DoubleType
          ),
          eventEntity as (
            "timestamp" typed DateType
          )
      )
  }
}
