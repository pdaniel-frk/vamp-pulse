package io.magnetic.vamp.pulse.storage.engine

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.{ElasticClient, FilterDefinition, QueryDefinition, SearchType}
import io.magnetic.vamp.pulse.api.{Aggregator, MetricQuery}
import io.magnetic.vamp.pulse.eventstream.decoder.EventDecoder
import io.magnetic.vamp.pulse.eventstream.producer.Event
import io.magnetic.vamp.pulse.mapper.CustomObjectSource
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation

import scala.collection.mutable.Queue
import scala.concurrent.{ExecutionContext, Future}


class MetricDAO(implicit client: ElasticClient, implicit val executionContext: ExecutionContext) {
  private val eventEntity = "event"
  private val eventIndex = "events"

  private val decoder = new EventDecoder()

  def insert(metric: Event) = {
    client.execute {
      index into s"$eventIndex/$eventEntity" doc CustomObjectSource(metric)
    }
  }


  //TODO: Figure out timestamp issues with elastic: We can only use epoch now + we get epoch as a double from elastic.
  def getEvents(metricQuery: MetricQuery): Future[Any] = {
    if(metricQuery.aggregator.isEmpty) {
      getPlainEvents(metricQuery)
    } else {
      getAggregateEvents(metricQuery)
    }
  }
  

  private def getPlainEvents(metricQuery: MetricQuery) = {
    val tagNum = metricQuery.tags.length

    val queries: Queue[QueryDefinition] = Queue(
      rangeQuery("timestamp") from metricQuery.time.from.toEpochSecond to metricQuery.time.to.toEpochSecond
    )

    if(tagNum > 0) queries += termsQuery("tags", metricQuery.tags:_*) minimumShouldMatch(tagNum)

    client.execute {
      search in eventIndex -> eventEntity query {
        must  (
          queries
        )
      } start 0 limit 30
    } map {
      resp => List(resp.getHits.hits().map((hit) =>  decoder.fromString(hit.sourceAsString())): _*)
    }
  }

  def getAggregateEvents(metricQuery: MetricQuery) = {

    val filters: Queue[FilterDefinition] = Queue(
      rangeFilter("timestamp") from metricQuery.time.from.toEpochSecond to metricQuery.time.to.toEpochSecond
    )

    if(!metricQuery.tags.isEmpty) filters += termsFilter("tags", metricQuery.tags :_*) execution("and")

    val aggregator = metricQuery.aggregator.getOrElse(Aggregator("average"))


    client.execute {
      search  in eventIndex -> eventEntity searchType SearchType.Count aggs {
        aggregation filter "filter_agg" filter {
          must(filters)
        } aggs {
          aggregator.`type` match {
            case "average" => aggregation avg "val_agg" field aggregator.field
            case "min" => aggregation min  "val_agg" field aggregator.field
            case "max" => aggregation max  "val_agg" field aggregator.field
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
      create index eventIndex mappings (
          eventEntity as (
            "tags" typed StringType,
            "timestamp" typed DateType,
            "value" typed ObjectType
          )
      )
  }
}
