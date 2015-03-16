package io.magnetic.vamp.pulse.storage.engine

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.sksamuel.elastic4s.{ElasticClient, FilterDefinition, QueryDefinition, SearchType}
import io.magnetic.vamp.pulse.api.{Aggregator, EventQuery}
import io.magnetic.vamp.pulse.eventstream.message.ElasticEvent
import io.magnetic.vamp.pulse.mapper.CustomObjectSource
import io.magnetic.vamp.pulse.util.Serializers
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection.mutable.Queue
import scala.concurrent.{ExecutionContext, Future}

final case class ResultList(list: List[ElasticEvent])
final case class AggregationResult(map: Map[String, Double])

class ElasticEventDAO(implicit client: ElasticClient, implicit val executionContext: ExecutionContext) {
  private val eventEntity = "event"
  private val eventIndex = "events"

  implicit val formats = Serializers.formats

  def insert(metric: ElasticEvent) = {
    client.execute {
      index into s"$eventIndex/$eventEntity" doc CustomObjectSource(metric)
    }
  }


  def getEvents(metricQuery: EventQuery): Future[Any] = {
    if(metricQuery.aggregator.isEmpty) {
      getPlainEvents(metricQuery)
    } else {
      getAggregateEvents(metricQuery)
    }
  }
  

  private def getPlainEvents(metricQuery: EventQuery) = {
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
      resp => ResultList(List(resp.getHits.hits().map((hit) =>  parse(hit.sourceAsString()).extract[ElasticEvent]): _*))
    }
  }

  def getAggregateEvents(metricQuery: EventQuery) = {

    val filters: Queue[FilterDefinition] = Queue(
      rangeFilter("timestamp") from metricQuery.time.from.toEpochSecond to metricQuery.time.to.toEpochSecond
    )

    if(!metricQuery.tags.isEmpty) filters += termsFilter("tags", metricQuery.tags :_*) execution "and"

    val aggregator = metricQuery.aggregator.getOrElse(Aggregator("average"))

    val aggFieldParts = List("value", metricQuery.`type`, aggregator.field)

    val aggField = aggFieldParts.filter(p => !p.isEmpty).mkString(".")


    client.execute {
      search  in eventIndex -> eventEntity searchType SearchType.Count aggs {
        aggregation filter "filter_agg" filter {
          must(filters)
        } aggs {
          aggregator.`type` match {
            case "average" => aggregation avg "val_agg" field aggField
            case "min" => aggregation min  "val_agg" field aggField
            case "max" => aggregation max  "val_agg" field aggField
            case str: String => throw new Exception(s"No such aggregation implemented $str")
          }
        }
      }
    } map {
      resp =>
        var value: Double = resp.getAggregations
        .get("filter_agg").asInstanceOf[InternalFilter]
        .getAggregations.get("val_agg").asInstanceOf[InternalNumericMetricsAggregation.SingleValue]
        .value()
        //TODO: Wrapper for result types to check corner-cases
        //TODO: Also might be a good idea not to use java api and map response json directly to whatever case classes we might have
        if(value.compareTo(Double.NaN) == 0) value = 0D

        AggregationResult(Map("value" -> value))
    }
  }
  
  def createIndex = client.execute {
      create index eventIndex mappings (
          eventEntity as (
            "tags" typed StringType,
            "timestamp" typed DateType,
            "value" typed ObjectType,
            "blob" typed ObjectType enabled false
          )
      )
  }
}
