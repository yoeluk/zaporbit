package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import securesocial.core.OAuth2Info

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

private[models] trait DAO {
  def adjust[A, B](m: Map[A, B], k: A)(f: B => B) = m.updated(k, f(m(k)))
  val users = TableQuery[Users]
  val offers = TableQuery[Offers]
  val shops = TableQuery[Shops]
  val pictures = TableQuery[Pictures]
  val locations = TableQuery[Locations]
  val transactions = TableQuery[Transactions]
  val buyings = TableQuery[Buyings]
  val sellings = TableQuery[Sellings]
  val billings = TableQuery[Billings]
  val conversations = TableQuery[Conversations]
  val messages = TableQuery[Messages]
  val feedbacks = TableQuery[Feedbacks]
  val ratings = TableQuery[Ratings]
  val merchants = TableQuery[Merchants]
  val friends = TableQuery[Friends]
  val oauth2info = TableQuery[OAuth2s]
  val listingStatuses = TableQuery[ListingStatuses]
  val userOptions = TableQuery[UserOptions]
}
