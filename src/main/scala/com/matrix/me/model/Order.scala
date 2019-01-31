package com.matrix.me.model

trait Order {
  val broker: String
  val qty: Double
  val side: Side
}

case class LimitOrder(broker: String, side: Side, qty: Double, limit: Double) extends Order

case class MarketOrder(broker: String, side: Side, qty: Double) extends Order
