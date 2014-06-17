package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

case class Merchant(id: Option[Long] = None,
                  userid: Long,
                  identifier: String,
                  secret: String,
                  created_on: Option[Timestamp] = None,
                  updated_on: Option[Timestamp] = None)

class Merchants(tag: Tag) extends Table[Merchant](tag, "Merchants") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userid = column[Long]("userid", O.NotNull)
  def identifier = column[String]("identifier", O.NotNull)
  def secret = column[String]("secret", O.NotNull)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, userid, identifier, secret, created_on.?, updated_on.?) <> (Merchant.tupled, Merchant.unapply _)
  def user = foreignKey("fk_merchant_user", userid, TableQuery[Users])(_.id)
}

object Merchants extends DAO {
  /**
   * Retrieve a rating with id
   * @param userid
   */
  def findByUserId(userid: Long)(implicit session: Session): Option[Merchant] =
    merchants.filter(_.userid === userid).firstOption

  /**
   * Insert a new rating
   * @param merchant
   */
  def insert(merchant: Merchant)(implicit session: Session): Unit =
    merchants.insert(merchant)

  def insertReturningId(merchant: Merchant)(implicit session: Session): Long =
    merchants returning merchants.map(_.id) insert merchant

  def update(id: Long, merchant: Merchant)(implicit session: Session): Unit = {
    val merchantToUpdate = merchant.copy(Some(id))
    merchants.filter(_.id === id).update(merchantToUpdate)
  }

}