package io.vamp.pulse.client

import java.time.OffsetDateTime

import io.vamp.pulse.model._

import scala.concurrent.Future

trait Aggregation {
  this: PulseClient =>

  def count(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime): Future[NumericAggregationResult] =
    count(tags, Some(from), Some(to))

  def count(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, field: Option[String] = None): Future[NumericAggregationResult] =
    aggregate(tags, from, to, Aggregator(Aggregator.Count, field))

  def max(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime): Future[NumericAggregationResult] =
    max(tags, Some(from), Some(to))

  def max(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, field: Option[String] = None): Future[NumericAggregationResult] =
    aggregate(tags, from, to, Aggregator(Aggregator.Max, field))

  def min(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime): Future[NumericAggregationResult] =
    min(tags, Some(from), Some(to))

  def min(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, field: Option[String] = None): Future[NumericAggregationResult] =
    aggregate(tags, from, to, Aggregator(Aggregator.Min, field))

  def average(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime): Future[NumericAggregationResult] =
    average(tags, Some(from), Some(to))

  def average(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, field: Option[String] = None): Future[NumericAggregationResult] =
    aggregate(tags, from, to, Aggregator(Aggregator.Average, field))

  def aggregate(tags: Set[String], from: Option[OffsetDateTime], to: Option[OffsetDateTime], aggregator: Aggregator): Future[NumericAggregationResult] =
    eventQuery[NumericAggregationResult](EventQuery(tags, Some(TimeRange(from, to)), Some(aggregator)))
}

