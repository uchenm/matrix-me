package com.matrix.me.model

sealed trait PriceLevel

case class LimitPrice(limit: Double) extends PriceLevel

case object MarketPrice extends PriceLevel
