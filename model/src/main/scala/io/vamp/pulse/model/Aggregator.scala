package io.vamp.pulse.model

import io.vamp.pulse.model.Aggregator.AggregatorType

import scala.language.implicitConversions

object Aggregator extends Enumeration {
  type AggregatorType = Aggregator.Value

  val min, max, average, count = Value
}

case class Aggregator(`type`: Option[AggregatorType], field: Option[String] = None)


trait AggregationResult

case class NumericAggregationResult(value: Double) extends AggregationResult
