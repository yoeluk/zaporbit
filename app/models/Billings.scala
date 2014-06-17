package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

case class Billing(id: Option[Long] = None,
                       status: String,
                       userid: Long,
                       offer_title: String,
                       offer_description: String,
                       offer_price: Double,
                       currency_code: String,
                       locale: String,
                       transactionid: Long,
                       paid_amount: Option[Double] = None,
                       googlewallet_id: Option[String] = None,
                       created_on: Option[Timestamp] = None,
                       updated_on: Option[Timestamp] = None)

class Billings(tag: Tag) extends Table[Billing](tag, "Billings") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def status = column[String]("status", O.NotNull)
  def userid = column[Long]("userid", O.NotNull)
  def offer_title = column[String]("offer_title", O.NotNull)
  def offer_description = column[String]("offer_description", O.NotNull)
  def offer_price = column[Double]("offer_price", O.NotNull)
  def currency_code = column[String]("currency_code", O.NotNull)
  def locale = column[String]("locale", O.NotNull)
  def transactionid = column[Long]("transactionid", O.NotNull)
  def paid_amount = column[Double]("paid_amount", O.Nullable)
  def googlewallet_id = column[String]("googlewallet_id", O.Nullable)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, status, userid, offer_title, offer_description, offer_price, currency_code, locale, transactionid, paid_amount.?, googlewallet_id.?, created_on.?, updated_on.?) <> (Billing.tupled, Billing.unapply _)
}

object Billings extends DAO {

  def count(id: Long)(implicit session: Session): Int =
    Query(billings.filter(_.userid === id).length).first

  def insert(billing: Billing)(implicit session: Session): Unit =
    billings.insert(billing)

  def delete(id: Long)(implicit session: Session): Unit =
    billings.filter(_.id === id).delete

  def findById(id: Long)(implicit session: Session): Option[Billing] =
    billings.filter(_.id === id).firstOption

  def findByUserId(id: Long)(implicit session: Session): Option[Billing] =
    billings.filter(_.userid === id).firstOption

  def billingsByUserId(userid: Long)(implicit session: Session): Map[String,List[Billing]] = {
    (for {
      b <- billings.filter(_.userid === userid)
    } yield b).sortBy(_.id.desc).list.groupBy(_.status)
  }

  def insertPaidBillWithOfferid(offerid: Long, price: Double, wallet_id: String)(implicit session: Session): Unit = {
    Offers.findById(offerid) match {
      case Some(offer) =>
        val bill = Billing(None,"paid",offer.userid,offer.title,offer.description,offer.price,"USD","en_US",0, Some(price), Some(wallet_id))
        billings.insert(bill)
      case None =>
    }
  }

  def unpaidBillsForUser(userid: Long)(implicit session: Session): Map[String, List[Billing]] = {
    billings.filter(_.userid === userid).filter(_.status === "unpaid").list.groupBy(_.currency_code)
  }

  def updatePaidBills(billIds: List[Long], wallet_id: String)(implicit session: Session): Unit = {
    billings.filter(_.id inSet billIds).map { row =>
      (row.status,row.googlewallet_id)
    }.update("paid",wallet_id)
  }

}