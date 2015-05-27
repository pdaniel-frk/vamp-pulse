package io.vamp.pulse.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import io.vamp.common.notification.{DefaultPackageMessageResolverProvider, LoggingNotificationProvider}
import io.vamp.pulse.http.PulseSerializationFormat
import io.vamp.pulse.model.{Aggregator, Event, EventQuery, TimeRange}
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import org.elasticsearch.search.aggregations.metrics.InternalNumericMetricsAggregation
import org.elasticsearch.search.sort.SortOrder
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, postfixOps}

final case class ElasticSearchResultList(list: List[Event])

final case class ElasticSearchAggregationResult(map: Map[String, Double])

class ElasticSearchEventDAO(implicit client: ElasticClient, implicit val executionContext: ExecutionContext)
  extends LoggingNotificationProvider with DefaultPackageMessageResolverProvider {

  private val eventEntity = "event"
  private val eventIndex = "events"

  implicit val formats = PulseSerializationFormat.serializer

  def getEvents(eventQuery: EventQuery): Future[Any] = {
    eventQuery.aggregator match {
      case None => getPlainEvents(eventQuery) map {
        resp => ElasticSearchResultList(List(resp.getHits.hits().map((hit) => parse(hit.sourceAsString()).extract[Event]): _*))
      }
      case Some(x: Aggregator) if x.`type` == Aggregator.count => getPlainEvents(eventQuery) map {
        resp => ElasticSearchAggregationResult(Map("value" -> resp.getHits.getTotalHits))
      }
      case Some(x: Aggregator) if x.`type` != Aggregator.count => getAggregateEvents(eventQuery)
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
    val aggregator = query.aggregator.getOrElse(Aggregator(Aggregator.average))
    val aggFieldParts = List("value", aggregator.field.getOrElse(""))
    val aggField = aggFieldParts.filter(p => !p.isEmpty).mkString(".")
    val qFilter = queryFilter(must(constructQuery(query)))


    client.execute {
      search in eventIndex -> eventEntity searchType SearchType.Count aggs {
        aggregation filter "filter_agg" filter {
          qFilter
        } aggs {
          aggregator.`type` match {
            case Aggregator.`average` => aggregation avg "val_agg" field aggField
            case Aggregator.`min` => aggregation min "val_agg" field aggField
            case Aggregator.`max` => aggregation max "val_agg" field aggField
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

        ElasticSearchAggregationResult(Map("value" -> value))
    }
  }
}

