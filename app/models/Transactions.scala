package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.functional.syntax._

case class Transaction(id: Option[Long] = None,
                       status: String,
                       offer_title: String,
                       offer_description: String,
                       offer_price: Double,
                       currency_code: String,
                       locale: String,
                       buyerid: Long,
                       sellerid: Long,
                       offerid: Long,
                       created_on: Option[Timestamp] = None,
                       updated_on: Option[Timestamp] = None)

class Transactions(tag: Tag) extends Table[Transaction](tag, "Transactions") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def status = column[String]("status", O.NotNull)
  def offer_title = column[String]("offer_title", O.NotNull)
  def offer_description = column[String]("offer_description", O.NotNull)
  def offer_price = column[Double]("offer_price", O.NotNull)
  def currency_code = column[String]("currency_code", O.NotNull)
  def locale = column[String]("locale", O.NotNull)
  def buyerid = column[Long]("buyerid", O.NotNull)
  def sellerid = column[Long]("sellerid", O.NotNull)
  def offerid = column[Long]("offerid", O.NotNull)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, status, offer_title, offer_description, offer_price, currency_code, locale, buyerid, sellerid, offerid, created_on.?, updated_on.?) <> (Transaction.tupled, Transaction.unapply _)
}

object Transactions extends DAO {
  import controllers.FormMappings._

  /*
   * reference from accepted answer in stackoverlow
   * http://stackoverflow.com/questions/17281291/how-do-i-write-a-json-format-for-an-object-in-the-java-library-that-doesnt-have
   */
  implicit val rds: Reads[Timestamp] = (__ \ "time").read[Long].map{ long => new Timestamp(long) }
  implicit val wrs: Writes[Timestamp] = (__ \ "time").write[Long].contramap{ (a: Timestamp) => a.getTime }
  implicit val fmt: Format[Timestamp] = Format(rds, wrs)

  implicit val billingFormat = Json.format[Billing]
  val billingForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "status" -> nonEmptyText,
      "userid" -> longNumber,
      "offer_title" -> nonEmptyText,
      "offer_description" -> nonEmptyText,
      "offer_price" -> of[Double],
      "locale" -> nonEmptyText,
      "currency_code" -> nonEmptyText,
      "transactionid" -> longNumber,
      "amount" -> of[Double],
      "payment_provider" -> optional(nonEmptyText),
      "pay_id" -> optional(nonEmptyText),
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Billing.apply)(Billing.unapply)
  )

  def insert(transaction: Transaction)(implicit session: Session): Unit =
    transactions.insert(transaction)

  def delete(id: Long)(implicit session: Session): Unit =
    transactions.filter(_.id === id).delete

  def findById(id: Long)(implicit session: Session): Option[Transaction] =
    transactions.filter(_.id === id).firstOption

  def findPendingOrProcessingTransById(offerid: Long)(implicit session: Session): Option[Transaction] =
    (for {
      t <- transactions if t.offerid === offerid &&
       ((t.status.toLowerCase like "%pending%") ||
        (t.status.toLowerCase like "%processing%"))
    } yield t).firstOption

  def insertReturningId(transaction: Transaction)(implicit session: Session): Option[Long] =
    Option(transactions returning transactions.map(_.id) insert transaction)

  def completeTransaction(transid: Long, userid: Long)(implicit session: Session): Boolean = {
    transactions.filter(_.id === transid).firstOption match {
      case None => false
      case Some(trans) =>
        if (trans.sellerid == userid) {
          transactions.filter(_.id === transid).map { row =>
            row.status
          }.update("completed")
          sellings.filter(_.transactionid === transid).map { row =>
            row.status
          }.update("completed")
          buyings.filter(_.transactionid === transid).map { row =>
            row.status
          }.update("completed")
          Json.obj(
            "status" -> "unpaid",
            "userid" -> trans.sellerid,
            "offer_title" ->  trans.offer_title,
            "offer_description" -> trans.offer_description,
            "offer_price" -> trans.offer_price,
            "locale" -> trans.locale,
            "currency_code" -> trans.currency_code,
            "transactionid" -> trans.id.get).validate[Billing].map { bill =>
            billings.insert(bill)
          }
          true
        } else false
    }
  }

  def failTransaction(transid: Long, userid: Long)(implicit session: Session): Boolean = {
    transactions.filter(_.id === transid).firstOption match {
      case None => false
      case Some(trans) =>
        if (trans.sellerid == userid || trans.buyerid == userid) {
          transactions.filter(_.id === transid).map {
            row =>
              row.status
          }.update("failed")
          sellings.filter(_.transactionid === transid).map {
            row =>
              row.status
          }.update("failed")
          buyings.filter(_.transactionid === transid).map {
            row =>
              row.status
          }.update("failed")
          true
        } else false
    }
  }

}