package io.vamp.pulse.old.storage.dao

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.mappings.FieldType._
import io.vamp.common.notification.{DefaultPackageMessageResolverProvider, LoggingNotificationProvider}
import io.vamp.pulse.model.{Event, Aggregator, EventQuery, TimeRange}
import io.vamp.pulse.notification.{EmptyEventError, MappingErrorNotification}
import io.vamp.pulse.old.mapper.CustomObjectSource
import io.vamp.pulse.old.util.Serializers
import org.elasticsearch.index.mapper.MapperParsingException
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.transport.RemoteTransportException
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

final case class ResultList(list: List[Event])

final case class AggregationResult(map: Map[String, Double])

class ElasticEventDAO(implicit client: ElasticClient, implicit val executionContext: ExecutionContext)
  extends LoggingNotificationProvider with DefaultPackageMessageResolverProvider {

  import CustomObjectSource._


  private val eventEntity = "event"
  private val eventIndex = "events"

  implicit val formats = Serializers.formats

  def insert(event: Event) = {
    if (event.tags.isEmpty) error(EmptyEventError)

    client.execute {
      insertQuery(event)
    } recoverWith {
      case e: RemoteTransportException => e.getCause match {
        case t: MapperParsingException => throw exception(MappingErrorNotification(e.getCause, event.schema.getOrElse("")))
      }
    }

  }

  private def insertQuery(event: Event): IndexDefinition = {
    index into s"$eventIndex/$eventEntity" doc event
  }

  def batchInsert(eventList: Seq[Event]) = {
    batchInsertFuture(eventList) await
  }

  def batchInsertFuture(eventList: Seq[Event]) = {
    client.execute {
      bulk(
        eventList.filter(_.tags.nonEmpty).map(event => insertQuery(event))
      )
    }
  }


  def getEvents(eventQuery: EventQuery): Future[Any] = {
    eventQuery.aggregator match {
      case None => getPlainEvents(eventQuery) map {
        resp => ResultList(List(resp.getHits.hits().map((hit) => parse(hit.sourceAsString()).extract[Event]): _*))
      }
      case Some(x: Aggregator) if x.`type` == Aggregator.Count => getPlainEvents(eventQuery) map {
        resp => AggregationResult(Map("value" -> resp.getHits.getTotalHits))
      }
      case Some(x: Aggregator) if x.`type` != Aggregator.Count => getAggregateEvents(eventQuery)
    }
  }

  private def constructQuery(eventQuery: EventQuery) = {
    val tagNum = eventQuery.tags.size

    val queries: mutable.Queue[QueryDefinition] = mutable.Queue(constructTimeQuery(eventQuery.time))

    if (tagNum > 0) queries += termsQuery("tags", eventQuery.tags.toSeq: _*) minimumShouldMatch tagNum

    queries
  }

  private def constructTimeQuery(timeRange: Option[TimeRange]) = timeRange match {
    case Some(TimeRange(Some(from), Some(to))) => rangeQuery("timestamp") from from.toEpochSecond to to.toEpochSecond
    case Some(TimeRange(None, Some(to))) => rangeQuery("timestamp") to to.toEpochSecond
    case Some(TimeRange(Some(from), None)) => rangeQuery("timestamp") from from.toEpochSecond
    case _ => rangeQuery("timestamp")
  }

  private def getPlainEvents(eventQuery: EventQuery) = {
    client.execute {
      search in eventIndex -> eventEntity query {
        must(
          constructQuery(eventQuery)
        )
      } sort (
        by field "timestamp" order SortOrder.DESC
        ) start 0 limit 30
    }
  }


  private def getAggregateEvents(query: EventQuery) = {
    val aggregator = query.aggregator.getOrElse(Aggregator(Aggregator.Average))
    val aggFieldParts = List("value", aggregator.field.getOrElse(""))
    val aggField = aggFieldParts.filter(p => !p.isEmpty).mkString(".")
    val qFilter = queryFilter(must(constructQuery(query)))


    client.execute {
      search in eventIndex -> eventEntity searchType SearchType.Count aggs {
        aggregation filter "filter_agg" filter {
          qFilter
        } aggs {
          aggregator.`type` match {
            case Aggregator.Average => aggregation avg "val_agg" field aggField
            case Aggregator.Min => aggregation min "val_agg" field aggField
            case Aggregator.Max => aggregation max "val_agg" field aggField
            case t: Aggregator.Value => throw new Exception(s"No such aggregation implemented $t")
          }
        }
      }
    } map {
      resp =>
        var value: Double = resp.getAggregations
          .get("filter_agg").asInstanceOf[InternalFilter]
          .getAggregations.get("val_agg").asInstanceOf[InternalNumericMetricsAggregation.SingleValue]
          .value()

        if (value.isNaN || value.isInfinite) value = 0D

        AggregationResult(Map("value" -> value))
    }
  }

  def createIndex = client.execute {
    create index eventIndex mappings (
      eventEntity as(
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
    } await (60 seconds)

    createIndex await
  }
}
