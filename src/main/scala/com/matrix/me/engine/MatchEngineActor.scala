package com.matrix.me.engine

import akka.actor.Actor
import akka.cluster.Cluster
import com.matrix.me.model._

class MatchEngineActor extends Actor{

  val cluster = Cluster(context.system)

  val ordertypes=OrderType.all()

  private var _referencePrice: Option[Double] = None

  def referencePrice = _referencePrice.get

  def referencePrice_=(price: Double) {
    _referencePrice = Some(price)
  }

  private val orderBooks = OrderBookFull(new OrderBook(Buy),new OrderBook(Buy))

  private val matchBuyOrder = new Matcher(orderBooks.buy, orderBooks.sell)
  private val matchSellOrder = new Matcher(orderBooks.sell, orderBooks.buy)


  def receive = {

    case order: Order =>
      order.side match {
        case Buy => matchBuyOrder(order)
        case Sell => matchSellOrder(order)
      }

    case _ =>
      println(s"I have been created at ${cluster.selfUniqueAddress}")
  }


  private class Matcher(book: OrderBook, counter: OrderBook) {

    def apply(order: Order) {
        val unfilledOrder = tryMatch(order)
        unfilledOrder.foreach(book.add(_))
    }


    private def tryMatch(order: Order): Option[Order] = {

      if (order.qty == 0) None
      else counter.top match {
        case None => Some(order)
        case Some(top) => tryMatchWithTop(order, top) match {
          case None => Some(order)
          case Some(trade) => {
            counter.decreaseTopBy(trade.qty)
            //          publish(trade)
            val unfilledOrder = ordertypes(order).decreasedBy(trade.qty)
            tryMatch(unfilledOrder)
          }
        }
      }
    }

    private def tryMatchWithTop(order: Order, top: Order): Option[Trade] = {
      def trade(price: Double) = {
        _referencePrice = Some(price)
        val (buy, sell) = if (order.side == Buy) (order, top) else (top, order)
        Some(Trade(buy.broker, sell.broker, price, math.min(buy.qty, sell.qty)))
      }

      lazy val oppositeBestLimit = {
        val oppositeBook = if (order.side == Buy) orderBooks.sell else orderBooks.buy
        oppositeBook.bestLimit
      }

      (order, top) match {

        case (_, topLimitOrder: LimitOrder) => {
          if (ordertypes(order).crossesAt(topLimitOrder.limit)) trade(topLimitOrder.limit)
          else None
        }

        case (limitOrder: LimitOrder, _: MarketOrder) => trade(oppositeBestLimit match {
          case Some(limit) => if (ordertypes(limitOrder).crossesAt(limit)) limit else limitOrder.limit
          case None => limitOrder.limit
        })

        case (_: MarketOrder, _: MarketOrder) => trade(oppositeBestLimit match {
          case Some(limit) => limit
          case None => _referencePrice match {
            case Some(price) => price
            case None => throw new IllegalStateException("Can't execute a trade with two market orders without best limit or reference price")
          }
        })
      }
    }
  }



}


case class OrderBookFull(buy: OrderBook, sell: OrderBook){
  def getBooks(side: Side): (OrderBook, OrderBook) = side match {
    case Buy => (buy, sell)
    case Sell => (sell, buy)
  }
}

case class Trade(buyingBroker: String, sellingBroker: String,     price: Double, qty: Double)

