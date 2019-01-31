package com.matrix.me.model

class OrderBook(side: Side)  {

  val orderTypes = OrderType.all()

  private var marketBook: List[Order] = Nil
  private var limitBook: List[(Double, List[Order])] = Nil
  private val priceOrdering = if (side == Sell) Ordering[Double] else Ordering[Double].reverse

  def add(order: Order) {

    orderTypes(order).price match {

      case MarketPrice => marketBook = marketBook :+ order
      case LimitPrice(limit) => addLimit(limit, order)
    }
  }

  def top: Option[Order] = marketBook match {
    case head :: _ => Some(head)
    case _ => limitBook.headOption.map({
      case (_, orders) => orders.head
    })
  }

  def bestLimit: Option[Double] = limitBook.headOption.map({
    case (limit, _) => limit
  })

  def decreaseTopBy(qty: Double) {

    marketBook match {
      case top :: tail => marketBook = if (qty == top.qty) tail else orderTypes(top).decreasedBy(qty) :: tail
      case _ => limitBook match {
        case ((level, orders) :: tail) => {
          val (top :: rest) = orders
          limitBook = (qty == top.qty, rest.isEmpty) match {
            case (true, true) => tail
            case (true, false) => (level, rest) :: tail
            case _ => (level, orderTypes(top).decreasedBy(qty) :: rest) :: tail
          }
        }
        case Nil => throw new IllegalStateException()
      }
    }
  }

  def orders(): List[Order] = marketBook ::: limitBook.flatMap({
    case (_, orders) => orders
  })

  private def addLimit(limit: Double, order: Order) {
    def insert(list: List[(Double, List[Order])]): List[(Double, List[Order])] = list match {
      case Nil => List((limit, List(order)))
      case (head@(bookLevel, orders)) :: tail => priceOrdering.compare(limit, bookLevel) match {
        case 0 => (bookLevel, orders :+ order) :: tail
        case n if n < 0 => (limit, List(order)) :: list
        case _ => head :: insert(tail)
      }
    }

    limitBook = insert(limitBook)
  }
}

object OrderBook {

  sealed trait Modifier {
    def decreaseTopBy(qty: Double)
  }

}
