package io.vamp.pulse.client

import java.time.OffsetDateTime

import io.vamp.pulse.model._

import scala.concurrent.Future

trait Aggregation {
  this: PulseClient =>

  def count(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime): Future[SingleValueAggregationResult] =
    count(tags, Some(from), Some(to))

  def count(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, field: Option[String] = None): Future[SingleValueAggregationResult] =
    aggregate(tags, from, to, Aggregator(Some(Aggregator.count), field))

  def max(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime): Future[SingleValueAggregationResult] =
    max(tags, Some(from), Some(to))

  def max(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, field: Option[String] = None): Future[SingleValueAggregationResult] =
    aggregate(tags, from, to, Aggregator(Some(Aggregator.max), field))

  def min(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime): Future[SingleValueAggregationResult] =
    min(tags, Some(from), Some(to))

  def min(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, field: Option[String] = None): Future[SingleValueAggregationResult] =
    aggregate(tags, from, to, Aggregator(Some(Aggregator.min), field))

  def average(tags: Set[String], from: OffsetDateTime, to: OffsetDateTime): Future[SingleValueAggregationResult] =
    average(tags, Some(from), Some(to))

  def average(tags: Set[String], from: Option[OffsetDateTime] = None, to: Option[OffsetDateTime] = None, field: Option[String] = None): Future[SingleValueAggregationResult] =
    aggregate(tags, from, to, Aggregator(Some(Aggregator.average), field))

  def aggregate(tags: Set[String], from: Option[OffsetDateTime], to: Option[OffsetDateTime], aggregator: Aggregator): Future[SingleValueAggregationResult] =
    eventQuery[SingleValueAggregationResult](EventQuery(tags, Some(TimeRange(from, to)), Some(aggregator)))
}

