package io.vamp.pulse.storage.dao

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.mappings.FieldType._
import io.vamp.common.notification.{DefaultPackageMessageResolverProvider, LoggingNotificationProvider}
import io.vamp.pulse.api.AggregatorType
import io.vamp.pulse.api.AggregatorType.AggregatorType
import io.vamp.pulse.api.{AggregatorType, Aggregator, EventQuery}
import io.vamp.pulse.eventstream.message.ElasticEvent
import io.vamp.pulse.eventstream.notification.MappingErrorNotification
import io.vamp.pulse.mapper.CustomObjectSource
import io.vamp.pulse.util.Serializers
import org.elasticsearch.index.mapper.MapperParsingException
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.RemoteTransportException
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection.mutable.Queue
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

final case class ResultList(list: List[ElasticEvent])
final case class AggregationResult(map: Map[String, Double])

class ElasticEventDAO(implicit client: ElasticClient, implicit val executionContext: ExecutionContext)
  extends LoggingNotificationProvider with DefaultPackageMessageResolverProvider{
  import CustomObjectSource._


  private val eventEntity = "event"
  private val eventIndex = "events"

  implicit val formats = Serializers.formats

  def insert(event: ElasticEvent) = {
    client.execute {
        insertQuery(event)
    } recoverWith  {
      case e: RemoteTransportException => e.getCause match {
        case t: MapperParsingException => throw exception(MappingErrorNotification(e.getCause, event.properties.objectType))
      }
    }

  }

  private def insertQuery(event: ElasticEvent): IndexDefinition = {
    index into s"$eventIndex/$eventEntity" doc event
  }

  def batchInsert(eventList: Seq[ElasticEvent]) = {
    batchInsertFuture(eventList) await
  }

  def batchInsertFuture(eventList: Seq[ElasticEvent]) = {
    client.execute {
      bulk(
        eventList.map(event => insertQuery(event))
      )
    }
  }


  def getEvents(eventQuery: EventQuery): Future[Any] = {
    if(eventQuery.aggregator.isEmpty) {
      getPlainEvents(eventQuery)
    } else {
      if(eventQuery.aggregator.get.`type` == AggregatorType.count) {
        getCount(eventQuery)
      } else getAggregateEvents(eventQuery)
    }
  }
  

  private def getPlainEvents(eventQuery: EventQuery) = {
    val tagNum = eventQuery.tags.length

    val queries: Queue[QueryDefinition] = Queue(
      rangeQuery("timestamp") from eventQuery.time.from.toEpochSecond to eventQuery.time.to.toEpochSecond
    )

    if(tagNum > 0) queries += termsQuery("tags", eventQuery.tags:_*) minimumShouldMatch(tagNum)

    if(!eventQuery.`type`.isEmpty) queries += termQuery("properties.objectType", eventQuery.`type`)

    client.execute {
      search in eventIndex -> eventEntity query {
        must  (
          queries
        )
      } sort (
        by field "timestamp" order SortOrder.DESC
      ) start 0 limit 30
    } map {
      resp => ResultList(List(resp.getHits.hits().map((hit) =>  parse(hit.sourceAsString()).extract[ElasticEvent]): _*))
    }
  }

  private def getCount(eventQuery: EventQuery) = {
    val tagNum = eventQuery.tags.length

    val queries: Queue[QueryDefinition] = Queue(
      rangeQuery("timestamp") from eventQuery.time.from.toEpochSecond to eventQuery.time.to.toEpochSecond
    )

    if(tagNum > 0) queries += termsQuery("tags", eventQuery.tags:_*) minimumShouldMatch(tagNum)

    if(!eventQuery.`type`.isEmpty) queries += termQuery("properties.objectType", eventQuery.`type`)

    client.execute {
      search in eventIndex -> eventEntity query {
        must  (
          queries
        )
      } 
    } map {
      resp => AggregationResult(Map("value" -> resp.getHits.getTotalHits))
    }
  }


  private def getAggregateEvents(metricQuery: EventQuery) = {

    val filters: Queue[FilterDefinition] = Queue(
      rangeFilter("timestamp") from metricQuery.time.from.toEpochSecond to metricQuery.time.to.toEpochSecond
    )

    if(!metricQuery.`type`.isEmpty) filters += termFilter("properties.objectType", metricQuery.`type`)

    if(!metricQuery.tags.isEmpty) filters += termsFilter("tags", metricQuery.tags :_*) execution "and"

    val aggregator = metricQuery.aggregator.getOrElse(Aggregator(AggregatorType.average))

    val aggFieldParts = List("value", metricQuery.`type`, aggregator.field)

    val aggField = aggFieldParts.filter(p => !p.isEmpty).mkString(".")


    client.execute {
      search  in eventIndex -> eventEntity searchType SearchType.Count aggs {
        aggregation filter "filter_agg" filter {
          must(filters)
        } aggs {
          aggregator.`type` match {
            case AggregatorType.average => aggregation avg "val_agg" field aggField
            case AggregatorType.min => aggregation min  "val_agg" field aggField
            case AggregatorType.max => aggregation max  "val_agg" field aggField
            case t: AggregatorType => throw new Exception(s"No such aggregation implemented $t")
          }
        }
      }
    } map {
      resp =>
        var value: Double = resp.getAggregations
        .get("filter_agg").asInstanceOf[InternalFilter]
        .getAggregations.get("val_agg").asInstanceOf[InternalNumericMetricsAggregation.SingleValue]
        .value()

        if(value.isNaN || value.isInfinite) value = 0D

        AggregationResult(Map("value" -> value))
    }
  }
  
  def createIndex = client.execute {
      create index eventIndex mappings (
          eventEntity as (
            "tags" typed StringType index NotAnalyzed,
            "timestamp" typed DateType,
            "value" typed ObjectType,
            "blob" typed ObjectType enabled false
          )
      )
  }

  def cleanupEvents = {
    client.execute {
      deleteIndex(eventIndex)
    } await(60 seconds)

    createIndex await
  }
}
