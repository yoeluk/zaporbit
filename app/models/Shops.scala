package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import java.sql.Timestamp
import play.api.db.slick.Config.driver.simple._

case class Shop(id: Option[Long] = None,
              name: String,
       description: String,
            userid: Long,
        created_on: Option[Timestamp] = None,
        updated_on: Option[Timestamp] = None)

class Shops(tag: Tag) extends Table[Shop](tag, "Shops") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def description = column[String]("description", O.NotNull)
  def userid = column[Long]("userid", O.NotNull)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, name, description, userid, created_on.?, updated_on.?) <> (Shop.tupled, Shop.unapply _)
  def user = foreignKey("fk_shop_user", userid, TableQuery[Users])(_.id)
}

object Shops extends DAO {
  /**
   * Retrieve a shop from the id
   * @param id
   */
  def findById(id: Long)(implicit session: Session): Option[Shop] =
    shops.filter(_.id === id).firstOption
  /**
   * Count all shops
   */
  def count(implicit session: Session): Int =
    Query(shops.length).first
  /**
   * Count shops with a filter
   * @param filter
   */
  def count(filter: String)(implicit session: Session): Int =
    Query(shops.filter(_.name.toLowerCase like filter.toLowerCase).length).first
  /**
   * Return a page of (Shop, User)
   * @param page
   * @param pageSize
   * @param orderBy
   * @param filter
   * @param session
   * @return
   */
  def list(page: Int = 0,
           pageSize: Int = 20,
           orderBy: Int = 1,
           filter: String = "%")(implicit session: Session): Page[(Shop, User)] = {
    val offset = pageSize * page
    val query = (for {
      s <- shops
      u <- s.user
      if s.name.toLowerCase like filter.toLowerCase
    } yield (
        s.id.?,
        s.name,
        s.description,
        s.created_on.?,
        s.updated_on.?,
        u.id.?,
        u.name,
        u.surname,
        u.fbuserid,
        u.email,
        u.isMerchant.?,
        u.created_on.?))
      .drop(offset)
      .take(pageSize)
    val totalRows = count(filter)
    val result = query.list.map { row => (
      Shop(row._1, row._2, row._3, row._6.get, row._4, row._5),
      User(row._6, row._7, row._8, row._9, row._10, row._11, row._12))
    }
    Page(result, page, offset, totalRows)
  }
  /**
   * Return a page of Shop
   * @param page
   * @param pageSize
   * @param orderBy
   * @param filter
   * @param session
   * @return
   */
  def list1(page: Int = 0,
            pageSize: Int = 20,
            orderBy: Int = 1,
            filter: String = "%")(implicit session: Session): Page[Shop] = {
    val offset = pageSize * page
    val query = (for {
      s <- shops
      if s.name.toLowerCase like filter.toLowerCase
    } yield (
        s.id.?,
        s.name,
        s.description,
        s.userid,
        s.created_on.?,
        s.updated_on.?))
      .drop(offset)
      .take(pageSize)
    val totalRows = count(filter)
    val result = query.list.map( row =>
      Shop(row._1, row._2, row._3, row._4, row._5, row._6))
    Page(result, page, offset, totalRows)
  }
  /**
   * Insert a new shop
   * @param shop
   */
  def insert(shop: Shop)(implicit session: Session): Unit =
    shops.insert(shop)
  /**
   * Update a shop
   * @param id
   * @param shop
   */
  def update(id: Long, shop: Shop)(implicit session: Session): Unit = {
    val shopToUpdate: Shop = shop.copy(Some(id))
    shops.filter(_.id === id).update(shopToUpdate)
  }
  /**
   * Delete a shop
   * @param id
   */
  def delete(id: Long)(implicit session: Session): Unit =
    shops.filter(_.id === id).delete
}
