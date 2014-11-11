package models

/**
 * Created by yoelusa on 07/11/14.
 */

import play.api.db.slick.Config.driver.simple._

case class PaypalMerchant(id: Option[Long] = None,
                          userid: Long,
                          token: String,
                          tokenSecret: String,
                          scope: String)

class PaypalMerchants(tag: Tag) extends Table[PaypalMerchant](tag, "PaypalMerchants") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userid = column[Long]("userid", O.NotNull)
  def token = column[String]("token", O.NotNull)
  def tokenSecret = column[String]("tokenSecret", O.NotNull)
  def scope = column[String]("scope", O.NotNull)
  def * = (id.?, userid, token, tokenSecret, scope) <> (PaypalMerchant.tupled, PaypalMerchant.unapply _)
  def user = foreignKey("fk_paypalmerchant_user", userid, TableQuery[Users])(_.id)
}

object PaypalMerchants extends DAO {

  def insert(merchant: PaypalMerchant)(implicit session: Session) =
    paypalMerchants.insert(merchant)

  def exist(userid: Long)(implicit session: Session) =
    paypalMerchants.filter(_.userid === userid).exists.run

  def delete(userid: Long)(implicit session: Session) =
    paypalMerchants.filter(_.userid === userid).delete

  def findByUserid(userid: Long)(implicit session: Session) =
    paypalMerchants.filter(_.userid === userid).firstOption

  def update(merchant: PaypalMerchant)(implicit session: Session) =
    paypalMerchants.filter(_.userid === merchant.userid).map { row =>
      (row.token, row.tokenSecret, row.scope)
    }.update(merchant.token, merchant.tokenSecret, merchant.scope)

  def insertOrUpdate(merchant: PaypalMerchant)(implicit session: Session) =
    this.findByUserid(merchant.userid) match {
      case None => this.insert(merchant)
      case Some(m) => this.update(m)
    }

}
