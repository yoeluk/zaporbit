package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

case class SellingTrans(id: Long,
                        offer_title: String,
                        offer_description: String,
                        offer_price: Double,
                        buyerid: Long,
                        offerid: Long,
                        transactionid: Long,
                        transStatus: String,
                        updated_on: Option[Timestamp])

case class Selling(id: Option[Long] = None,
                        status: String,
                        userid: Long,
                        transactionid: Long,
                        created_on: Option[Timestamp] = None,
                        updated_on: Option[Timestamp] = None)

class Sellings(tag: Tag) extends Table[Selling](tag, "Sellings") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def status = column[String]("status", O.NotNull)
  def userid = column[Long]("userid", O.NotNull)
  def transactionid = column[Long]("transactionid", O.Nullable)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, status, userid, transactionid, created_on.?, updated_on.?) <> (Selling.tupled, Selling.unapply _)
  def transaction = foreignKey("fk_selling_transaction", transactionid, TableQuery[Transactions])(_.id)
}

object Sellings extends DAO {

  def count(id: Long)(implicit session: Session): Int =
    Query(sellings.filter(_.userid === id).length).first

  def insert(selling: Selling)(implicit session: Session): Unit =
    sellings.insert(selling)

  def delete(id: Long)(implicit session: Session): Unit =
    sellings.filter(_.id === id).delete

  def findById(id: Long)(implicit session: Session): Option[Selling] =
    sellings.filter(_.id === id).firstOption

  def findByUserId(id: Long)(implicit session: Session): Option[Selling] =
    sellings.filter(_.userid === id).firstOption

  def findByTransid(transid: Long)(implicit session: Session): Option[Selling] =
    sellings.filter(_.transactionid === transid).firstOption

  /**
   *
   * @param page
   * @param pageSize
   * @param userid
   * @param session
   * @return
   */
  def sellingTransByUserId(page: Int = 0,
                          pageSize: Int = 25,
                          userid: Long)(implicit session: Session): Page[SellingTrans] = {
    val offset = pageSize * page
    val query = (for {
      s <- sellings.filter(_.userid === userid)
      t <- s.transaction
    } yield (
        s.id.?,
        t.offer_title,
        t.offer_description,
        t.offer_price,
        t.buyerid,
        t.offerid,
        s.transactionid,
        t.status,
        t.updated_on.?)
      ).sortBy(_._1.desc).drop(offset).take(pageSize)
    val totalRows = count(userid)
    val result = query.list.map { row =>
      SellingTrans(row._1.get, row._2, row._3, row._4, row._5, row._6, row._7, row._8, row._9)
    }
    Page(result, page, offset, totalRows)
  }

  /**
   *
   * @param transid
   * @param session
   * @return
   */
  def acceptSellingTrans(transid: Long)(implicit session: Session): Unit = {
    transactions.filter(_.id === transid).map { row =>
      row.status
    }.update("accepted")
    sellings.filter(_.transactionid === transid).map { row =>
      row.status
    }.update("accepted")
    buyings.filter(_.transactionid === transid).map { row =>
      row.status
    }.update("accepted")
  }

}