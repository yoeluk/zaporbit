package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

case class BuyingTrans(id: Long,
                        offer_title: String,
                        offer_description: String,
                        offer_price: Double,
                        sellerid: Long,
                        offerid: Long,
                        transactionid: Long,
                        transStatus: String,
                        updated_on: Option[Timestamp])

case class Buying(id: Option[Long] = None,
                       status: String,
                       userid: Long,
                       transactionid: Long,
                       created_on: Option[Timestamp] = None,
                       updated_on: Option[Timestamp] = None)

class Buyings(tag: Tag) extends Table[Buying](tag, "Buyings") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def status = column[String]("status", O.NotNull)
  def userid = column[Long]("userid", O.NotNull)
  def transactionid = column[Long]("transactionid", O.Nullable)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, status, userid, transactionid, created_on.?, updated_on.?) <> (Buying.tupled, Buying.unapply _)
  def transaction = foreignKey("fk_buying_transaction", transactionid, TableQuery[Transactions])(_.id)
}

object Buyings extends DAO {

  def count(id: Long)(implicit session: Session): Int =
    Query(buyings.filter(_.userid === id).length).first

  def insert(buying: Buying)(implicit session: Session): Unit =
    buyings.insert(buying)

  def delete(id: Long)(implicit session: Session): Unit =
    buyings.filter(_.userid === id).delete

  def findById(id: Long)(implicit session: Session): Option[Buying] =
    buyings.filter(_.id === id).firstOption

  def findByTransid(transid: Long)(implicit session: Session): Option[Buying] =
    buyings.filter(_.transactionid === transid).firstOption

  def findByUserId(id: Long)(implicit session: Session): Option[Buying] =
    buyings.filter(_.userid === id).firstOption

  def buyingTransByUserId(page: Int = 0,
                          pageSize: Int = 25,
                          userid: Long)(implicit session: Session): Page[BuyingTrans] = {
    val offset = pageSize * page
    val query = (for {
      b <- buyings.filter(_.userid === userid)
      t <- b.transaction
    } yield (
        b.id.?,
        t.offer_title,
        t.offer_description,
        t.offer_price,
        t.sellerid,
        t.offerid,
        b.transactionid,
        t.status,
        t.updated_on.?)
      ).sortBy(_._1.desc).drop(offset).take(pageSize)
    val totalRows = count(userid)
    val result = query.list.map { row =>
      BuyingTrans(row._1.get, row._2, row._3, row._4, row._5, row._6, row._7, row._8, row._9)
    }
    Page(result, page, offset, totalRows)
  }

}