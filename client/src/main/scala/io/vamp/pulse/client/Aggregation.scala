package io.vamp.pulse.client

import java.time.OffsetDateTime

import io.vamp.pulse.model._

import scala.concurrent.Future

trait Aggregation {
  this: PulseClient =>

  def count(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime, includeLower: Boolean, includeUpper: Boolean): Future[SingleValueAggregationResult] =
    count(tags, Some(from), Some(to), includeLower, includeUpper)

  def count(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, includeLower: Boolean = true, includeUpper: Boolean = true, field: Option[String] = None): Future[SingleValueAggregationResult] =
    aggregate(tags, from, to, includeLower, includeUpper, Aggregator(Some(Aggregator.count), field))

  def max(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime, includeLower: Boolean, includeUpper: Boolean): Future[SingleValueAggregationResult] =
    max(tags, Some(from), Some(to), includeLower, includeUpper)

  def max(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, includeLower: Boolean = true, includeUpper: Boolean = true, field: Option[String] = None): Future[SingleValueAggregationResult] =
    aggregate(tags, from, to, includeLower, includeUpper, Aggregator(Some(Aggregator.max), field))

  def min(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime, includeLower: Boolean, includeUpper: Boolean): Future[SingleValueAggregationResult] =
    min(tags, Some(from), Some(to), includeLower, includeUpper)

  def min(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, includeLower: Boolean = true, includeUpper: Boolean = true, field: Option[String] = None): Future[SingleValueAggregationResult] =
    aggregate(tags, from, to, includeLower, includeUpper, Aggregator(Some(Aggregator.min), field))

  def average(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime, includeLower: Boolean, includeUpper: Boolean): Future[SingleValueAggregationResult] =
    average(tags, Some(from), Some(to), includeLower, includeUpper)

  def average(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, includeLower: Boolean = true, includeUpper: Boolean = true, field: Option[String] = None): Future[SingleValueAggregationResult] =
    aggregate(tags, from, to, includeLower, includeUpper, Aggregator(Some(Aggregator.average), field))

  def aggregate(tags: Set[String], from: Option[OffsetDateTime], to: Option[OffsetDateTime], includeLower: Boolean, includeUpper: Boolean, aggregator: Aggregator): Future[SingleValueAggregationResult] =
    eventQuery[SingleValueAggregationResult](EventQuery(tags, Some(TimeRange.from(from, to, includeLower, includeUpper)), Some(aggregator)))
}

