package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

case class ZOLocation(street: String,
                      locality: String,
                      administrativeArea: String,
                      latitude: Double,
                      longitude: Double)

case class Location(id: Option[Long] = None,
                    street: String,
                    locality: String,
                    administrativeArea: String,
                    latitude: Double,
                    longitude: Double,
                    offerid: Option[Long],
                    created_on: Option[Timestamp] = None,
                    updated_on: Option[Timestamp] = None)

class Locations(tag: Tag) extends Table[Location](tag, "Locations") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def street = column[String]("street", O.NotNull)
  def locality = column[String]("locality", O.NotNull)
  def administrativeArea = column[String]("adminArea", O.NotNull)
  def latitude = column[Double]("latitude", O.NotNull)
  def longitude = column[Double]("longitude", O.NotNull)
  def offerid = column[Long]("offerid", O.Nullable)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, street, locality, administrativeArea, latitude, longitude, offerid.?, created_on.?, updated_on.?) <> (Location.tupled, Location.unapply _)
  def offer = foreignKey("fk_location_offer", offerid.?, TableQuery[Offers])(_.id)
}

object Locations extends DAO {

  def count(locality: String, adminArea: String)(implicit session: Session): Int =
    Query(locations
      .filter(_.locality.toLowerCase like locality.toLowerCase)
      .filter(_.administrativeArea.toLowerCase === adminArea.toLowerCase)
      .length).first

  def countFiltered(locality: String, adminArea: String, filter: String)(implicit session: Session): Int =  {
    Query(
      (for {
        l <- locations.filter(_.locality.toLowerCase === locality.toLowerCase)
          .filter(_.administrativeArea.toLowerCase === adminArea.toLowerCase)
        o <- l.offer if o.title.toLowerCase like "%"+filter.toLowerCase+"%"
      } yield o).length).first
  }

  /**
   *
   * @param page page
   * @param pageSize page size
   * @param radius radius around latlng
   * @param loc zo location
   * @param session bd session
   * @return
   */
  def listByLoc(page: Int = 0,
            pageSize: Int = 25,
              radius: Int = 10,
                 loc: ZOLocation,
              userid: Long)(implicit session: Session): (Page[(Listing, ZOLocation, User)], Map[Long, (Int, Int)]) = {

    val offset = pageSize * page

    val friendsLQ = for {
      f <- friends.filter(_.userid === userid)
      o <- offers.filter(_.userid === f.friendid)
      l <- locations if l.locality =!= loc.locality
      u <- o.user
    } yield ((
        o.id.?,
        o.title,
        o.description,
        o.price,
        o.locale,
        o.shop,
        o.highlight,
        o.waggle,
        o.telephone.?,
        o.userid,
        o.created_on.?,
        o.updated_on.?),
        (l.street,
          l.locality,
          l.administrativeArea,
          l.latitude,
          l.longitude),
        (u.id.?,
          u.name,
          u.surname,
          u.fbuserid,
          u.email,
          u.isMerchant.?,
          u.created_on.?))

    val q = ((for {
      l <- locations if
        l.locality.toLowerCase === loc.locality.toLowerCase &&
        l.administrativeArea.toLowerCase === loc.administrativeArea.toLowerCase
      o <- l.offer
      u <- o.user
    } yield (
        (o.id.?,
        o.title,
        o.description,
        o.price,
        o.locale,
        o.shop,
        o.highlight,
        o.waggle,
        o.telephone.?,
        o.userid,
        o.created_on.?,
        o.updated_on.?),
        (l.street,
          l.locality,
          l.administrativeArea,
          l.latitude,
          l.longitude),
        (u.id.?,
          u.name,
          u.surname,
          u.fbuserid,
          u.email,
          u.isMerchant.?,
          u.created_on.?))
      ) ++ friendsLQ).sortBy(_._1._11.desc).drop(offset).take(pageSize).list
    val totalRows = this.count(loc.locality, loc.administrativeArea)
    val result = q.map { row =>
      (Listing(row._1._1, row._1._2, row._1._3, row._1._4, row._1._5,
        Option((for {
          p <- pictures if p.offerid === row._1._1.get
        } yield p.name).list), row._1._6, row._1._7, row._1._8, row._1._9, row._1._10, row._1._11, row._1._12),
        ZOLocation(row._2._1, row._2._2, row._2._3, row._2._4, row._2._5),
        User(row._3._1, row._3._2, row._3._3, row._3._4, row._3._5, row._3._6, row._3._7)
        )}
    val uniqueRatings = this.calcRatingForUsers(result)
    (Page(result, page, offset, totalRows), uniqueRatings)
  }

  /**
   *
   * @param page
   * @param pageSize
   * @param loc
   * @param filterStr
   * @param session
   * @return
   */
  def filterLoc(page: Int = 0,
                pageSize: Int = 20,
                loc: ZOLocation,
                filterStr: String = "%",
                userid: Long)(implicit session: Session): (Page[(Listing, ZOLocation, User)], Map[Long, (Int, Int)]) = {
    val filters = filterStr.split("\\s+").filterNot(_ == " ").map( x => x.toLowerCase).toVector
    //println( filters.count(_.r.findAllIn("the dog").length > 0) )

    val friendsLQ = for {
      f <- friends.filter(_.userid === userid)
      o <- offers.filter(_.userid === f.friendid)
      l <- locations if l.locality =!= loc.locality
      u <- o.user
    } yield (
        (o.id.?,
        o.title,
        o.description,
        o.price,
        o.locale,
        o.shop,
        o.highlight,
        o.waggle,
        o.telephone.?,
        o.userid,
        o.created_on.?,
        o.updated_on.?),
        (l.street,
          l.locality,
          l.administrativeArea,
          l.latitude,
          l.longitude),
        (u.id.?,
          u.name,
          u.surname,
          u.fbuserid,
          u.email,
          u.isMerchant.?,
          u.created_on.?))

    val offset = pageSize * page
    val q = ((for {
      l <- locations.filter(_.locality.toLowerCase === loc.locality.toLowerCase)
        .filter(_.administrativeArea.toLowerCase === loc.administrativeArea.toLowerCase)
      o <- l.offer
      u <- o.user
    } yield ((
        o.id.?,
        o.title,
        o.description,
        o.price,
        o.locale,
        o.shop,
        o.highlight,
        o.waggle,
        o.telephone.?,
        o.userid,
        o.created_on.?,
        o.updated_on.?),
        (l.street,
          l.locality,
          l.administrativeArea,
          l.latitude,
          l.longitude),
        (u.id.?,
          u.name,
          u.surname,
          u.fbuserid,
          u.email,
          u.isMerchant.?,
          u.created_on.?))) ++ friendsLQ).sortBy(_._1._11.desc)
      .list.filter( x => filters.count(
        _.r findFirstIn x._1._2.toLowerCase match {
          case None => false
          case _ => true
        }) == filters.length )
    val totalRows = q.length
    q.drop(offset).take(pageSize)
    val result = q.map { row =>
      (Listing(row._1._1, row._1._2, row._1._3, row._1._4, row._1._5,
        Option((for {
          p <- pictures if p.offerid === row._1._1.get
        } yield p.name).list), row._1._6, row._1._7, row._1._8, row._1._9, row._1._10, row._1._11, row._1._12),
        ZOLocation(row._2._1, row._2._2, row._2._3, row._2._4, row._2._5),
        User(row._3._1, row._3._2, row._3._3, row._3._4, row._3._5, row._3._6, row._3._7)
        )}
    val uniqueRatings = this.calcRatingForUsers(result)
    (Page(result, page, offset, totalRows), uniqueRatings)
  }
  /**
   * Retrieve a location
   * @param id
   */

  def findZLocByOfferId(id: Long)(implicit session: Session): Option[Location] =
    locations.filter(_.offerid === id).firstOption

  def findById(id: Long)(implicit session: Session): Option[Location] =
    locations.filter(_.id === id).firstOption
  /**
   * Insert a new location
   * @param location
   */
  def insert(location: Location)(implicit session: Session): Unit =
    locations.insert(location)
  /**
   * Delete a location
   * @param id
   */
  def delete(id: Long)(implicit session: Session): Unit =
    locations.filter(_.id === id).delete

  def calcRatingForUsers(page: List[(Listing, ZOLocation, User)])(implicit session: Session): Map[Long, (Int, Int)] = {
    val diffUsers = page.foldLeft(Nil: List[Long]) {
      (acc, next) => if (acc contains next._3.id.get) acc else next._3.id.get :: acc
    }
    val ratings = (for {
      rt <- TableQuery[Ratings].filter(_.id inSet diffUsers)
    } yield rt.userid -> rt.rating).toMap
    ratings.foldLeft(Map(): Map[Long, (Int, Int)]) {
      case (a, (k, v)) => if (a contains k) adjust(a, k){case (v1,c) => (v+v1,c+1)} else a + (k -> (v, 1))
    }
  }


  /*************** TESTING METHODS ********************/

  /**
   *
   * @param page page
   * @param pageSize page size
   * @param radius radius around latlng
   * @param loc zo location
   * @param session bd session
   * @return
   */
  def testListByLoc(page: Int = 0,
                pageSize: Int = 25,
                radius: Int = 10,
                loc: ZOLocation,
                userid: Long)(implicit session: Session): (Page[(Listing, ZOLocation, User)], Map[Long, (Int, Int)]) = {

    val testPage = 0
    val testOffset = pageSize * testPage
    val offset = pageSize * page

    val friendsLQ = for {
      f <- friends.filter(_.userid === userid)
      o <- offers.filter(_.userid === f.friendid)
      l <- locations if l.locality =!= loc.locality
      u <- o.user
    } yield ((
        o.id.?,
        o.title,
        o.description,
        o.price,
        o.locale,
        o.shop,
        o.highlight,
        o.waggle,
        o.telephone.?,
        o.userid,
        o.created_on.?,
        o.updated_on.?),
        (l.street,
          l.locality,
          l.administrativeArea,
          l.latitude,
          l.longitude),
        (u.id.?,
          u.name,
          u.surname,
          u.fbuserid,
          u.email,
          u.isMerchant.?,
          u.created_on.?))

    val q = ((for {
      l <- locations if
    l.locality.toLowerCase === loc.locality.toLowerCase &&
      l.administrativeArea.toLowerCase === loc.administrativeArea.toLowerCase
      o <- l.offer
      u <- o.user
    } yield (
        (o.id.?,
          o.title,
          o.description,
          o.price,
          o.locale,
          o.shop,
          o.highlight,
          o.waggle,
          o.telephone.?,
          o.userid,
          o.created_on.?,
          o.updated_on.?),
        (l.street,
          l.locality,
          l.administrativeArea,
          l.latitude,
          l.longitude),
        (u.id.?,
          u.name,
          u.surname,
          u.fbuserid,
          u.email,
          u.isMerchant.?,
          u.created_on.?))
      ) ++ friendsLQ).sortBy(_._1._11.desc).drop(testOffset).take(pageSize).list
    val totalRows = this.count(loc.locality, loc.administrativeArea)
    val result = q.map { row =>
      (Listing(row._1._1, row._1._2, row._1._3, row._1._4, row._1._5,
        Option((for {
          p <- pictures if p.offerid === row._1._1.get
        } yield p.name).list), row._1._6, row._1._7, row._1._8, row._1._9, row._1._10, row._1._11, row._1._12),
        ZOLocation(row._2._1, row._2._2, row._2._3, row._2._4, row._2._5),
        User(row._3._1, row._3._2, row._3._3, row._3._4, row._3._5, row._3._6, row._3._7)
        )}
    val uniqueRatings = this.calcRatingForUsers(result)
    (Page(result, page, offset, totalRows), uniqueRatings)
  }

}
