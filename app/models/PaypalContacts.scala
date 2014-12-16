package models

/**
 * Created by yoelusa on 07/11/14.
 */

import play.api.db.slick.Config.driver.simple._

case class PaypalContant(id: Option[Long] = None,
                         merchantid: Long,
                         name: String,
                         surname: String,
                         email: String,
                         businessName: Option[String],
                         country: String,
                         paypalid: String)

class PaypalContants(tag: Tag) extends Table[PaypalContant](tag, "PaypalContants") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def merchantid = column[Long]("merchantid", O.NotNull)
  def name = column[String]("name", O.NotNull)
  def surname = column[String]("surname", O.NotNull)
  def email = column[String]("email", O.NotNull)
  def businessName = column[String]("businessName", O.Nullable)
  def country = column[String]("country", O.NotNull)
  def paypalid = column[String]("paypalid", O.NotNull)
  def * = (id.?, merchantid, name, surname, email, businessName.?, country, paypalid) <> (PaypalContant.tupled, PaypalContant.unapply _)
  def merchant = foreignKey("fk_contact_paypalmerchant", merchantid, TableQuery[PaypalMerchants])(_.id)
}

object PaypalContants extends DAO {

  def insert(contact: PaypalContant)(implicit session: Session) =
    paypalContants.insert(contact)

  def exist(merchantid: Long)(implicit session: Session) =
    paypalContants.filter(_.merchantid === merchantid).exists.run

  def delete(merchantid: Long)(implicit session: Session) =
    paypalContants.filter(_.merchantid === merchantid).delete

  def findByMerchantid(merchantid: Long)(implicit session: Session) =
    paypalContants.filter(_.merchantid === merchantid).firstOption

  def update(contact: PaypalContant)(implicit session: Session) =
    paypalContants.filter(_.merchantid === contact.merchantid).update(contact)

  def insertOrUpdate(contact: PaypalContant)(implicit session: Session) =
    this.findByMerchantid(contact.merchantid) match {
      case None => this.insert(contact)
      case Some(c) => this.update(contact.copy(id = c.id))
    }
}