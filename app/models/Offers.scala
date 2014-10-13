package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import java.sql.Timestamp
import play.api.db.slick.Config.driver.simple._

trait OfferTrait {
  def id: Option[Long]
  def title: String
  def description: String
  def price: Double
  def shop: String
  def highlight: Boolean
  def waggle: Boolean
  def userid: Long
  def created_on: Option[Timestamp]
  def updated_on: Option[Timestamp]
}

case class Offer(id: Option[Long] = None,
                 title: String,
                 description: String,
                 price: Double,
                 locale: String,
                 currency_code: String,
                 shop: String,
                 highlight: Boolean,
                 waggle: Boolean,
                 telephone: Option[String],
                 userid: Long,
                 created_on: Option[Timestamp] = None,
                 updated_on: Option[Timestamp] = None)
  extends OfferTrait

case class Listing(id: Option[Long] = None,
                   title: String,
                   description: String,
                   price: Double,
                   locale: String,
                   currency_code: String,
                   pictures: Option[List[String]],
                   shop: String,
                   highlight: Boolean,
                   waggle: Boolean,
                   telephone: Option[String],
                   userid: Long,
                   created_on: Option[Timestamp] = None,
                   updated_on: Option[Timestamp] = None)
  extends OfferTrait

case class OfferStatus(status: String)

case class OfferPicture(name: String)

class Offers(tag: Tag) extends Table[Offer](tag, "Offers") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title", O.NotNull)
  def price = column[Double]("price", O.NotNull)
  def locale = column[String]("locale", O.NotNull)
  def currency_code = column[String]("currency_code", O.NotNull)
  def shop = column[String]("shop", O.NotNull)
  def highlight = column[Boolean]("highlight", O.NotNull)
  def waggle = column[Boolean]("waggle", O.NotNull)
  def description = column[String]("description", O.NotNull)
  def telephone = column[String]("telephone", O.NotNull)
  def userid = column[Long]("userid", O.NotNull)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, title, description, price, locale, currency_code, shop, highlight, waggle, telephone.?, userid, created_on.?, updated_on.?) <> (Offer.tupled, Offer.unapply _)
  def user = foreignKey("fk_offer_user", userid, TableQuery[Users])(_.id)
}

object Offers extends DAO {
  /**
   * Retrieve a computer from the id
   * @param id
   */
  def findById(id: Long)(implicit session: Session): Option[Offer] =
    offers.filter(_.id === id).firstOption

  def listingWithOffer(offer: Offer)(implicit session: Session): Listing =
    Listing(
      id = offer.id,
      title = offer.title,
      description = offer.description,
      price = offer.price,
      locale = offer.locale,
      currency_code = offer.currency_code,
      pictures = Option((for {
        p <- pictures.filter(_.offerid === offer.id.get)
      } yield p.name).list),
      shop = offer.shop,
      highlight = offer.highlight,
      waggle = offer.waggle,
      telephone = offer.telephone,
      userid = offer.userid,
      created_on = offer.created_on,
      updated_on = offer.updated_on)

  def findListingById(id: Long)(implicit session: Session): Option[Listing] = {
    offers.filter(_.id === id).firstOption match {
      case Some(o) =>
        Option(Listing(
          o.id,
          o.title,
          o.description,
          o.price,
          o.locale,
          o.currency_code,
          Option((for {
            p <- pictures.filter(_.offerid === o.id.get)
          } yield p.name).list),
          o.shop,
          o.highlight,
          o.waggle,
          o.telephone,
          o.userid,
          o.created_on,
          o.updated_on
        ))
      case None =>
        None
    }
  }

  /**
   * Count all offers
   */
  def count(implicit session: Session): Int =
    Query(offers.length).first
  /**
   * Count offers with a filter
   * @param filter
   */
  def count(filter: String)(implicit session: Session): Int =
    Query(offers.filter(_.title.toLowerCase like filter.toLowerCase).length).first
  /**
   *
   * @param userId
   * @param session
   * @return
   */
  def offerCountOfUser(userId: Long)(implicit session: Session): Int =
    Query(offers.filter(_.id === userId).length).first
  /**
   * Return a page of (Listing, User)
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
           filter: String = "%")(implicit session: Session): Page[(Listing, User)] = {
    val offset = pageSize * page
    val query = (for {
      o <- offers
      u <- o.user if u.email.toLowerCase like filter.toLowerCase
      } yield (
        o.id.?,
        o.title,
        o.description,
        o.price,
        o.locale,
        o.currency_code,
        o.shop,
        o.highlight,
        o.waggle,
        o.telephone.?,
        o.userid,
        o.created_on.?,
        o.updated_on.?,
        u.id.?,
        u.name,
        u.surname,
        u.fbuserid,
        u.email,
        u.isMerchant.?,
        u.created_on.?)
      ).sortBy(_._10.desc).drop(offset).take(pageSize)
    val totalRows = count(filter)
    val result = query.list.map {
      row => {(
        Listing(row._1, row._2, row._3, row._4, row._5, row._6,
          Option((for {
            p <- pictures if p.offerid === row._1.get
              } yield p.name).list), row._7, row._8, row._9, row._10, row._11, row._12, row._13),
        User(row._14, row._15, row._16, row._17, row._18, row._19, row._20)
        )}
    }
    Page(result, page, offset, totalRows)
  }
  /**
   * Return a page of Listing
   * @param page
   * @param pageSize
   * @param orderBy
   * @param userId
   * @param session
   * @return
   */
  def list1(page: Int = 0,
            pageSize: Int = 20,
            orderBy: Int = 1,
            userId: Long = 1)(implicit session: Session): Page[Listing] = {
    val offset = pageSize * page
    val query = (for {
      o <- offers.filter(_.userid === userId)
    } yield (
        o.id.?,
        o.title,
        o.description,
        o.price,
        o.locale,
        o.currency_code,
        o.shop,
        o.highlight,
        o.waggle,
        o.telephone.?,
        o.userid,
        o.created_on.?,
        o.updated_on.?)
    ).sortBy(_._13.desc).drop(offset).take(pageSize)
    val totalRows = offerCountOfUser(userId)
    val result = query.list.map( row =>
      Listing(row._1, row._2, row._3, row._4, row._5, row._6,
        Option((for {
          p <- pictures if p.offerid === row._1.get
        } yield p.name).list), row._7, row._8, row._9, row._10, row._11, row._12, row._13))
    Page(result, page, offset, totalRows)
  }

  def list2(page: Int = 0,
            pageSize: Int = 20,
            orderBy: Int = 1,
            userId: Long = 1)(implicit session: Session): Page[Listing] = {
    val offset = pageSize * page
    val query = (for {
      o <- offers.filter(_.userid === userId)
      s <- listingStatuses.filter(_.offerid === o.id).filter(_.status === "forsale")
    } yield (
        o.id.?,
        o.title,
        o.description,
        o.price,
        o.locale,
        o.currency_code,
        o.shop,
        o.highlight,
        o.waggle,
        o.telephone.?,
        o.userid,
        o.created_on.?,
        o.updated_on.?)
      ).sortBy(_._13.desc).drop(offset).take(pageSize)
    val totalRows = offerCountOfUser(userId)
    val result = query.list.map( row =>
      Listing(row._1, row._2, row._3, row._4, row._5, row._6,
        Option((for {
          p <- pictures if p.offerid === row._1.get
        } yield p.name).list), row._7, row._8, row._9, row._10, row._11, row._12, row._13))
    Page(result, page, offset, totalRows)
  }

  /**
   *
   * @param page
   * @param pageSize
   * @param orderBy
   * @param userId
   * @param session
   * @return
   */
  def offersForUser(page: Int = 0,
                    pageSize: Int = 20,
                    orderBy: Int = 1,
                    userId: Long = 1)(implicit session: Session): Page[(Offer, OfferStatus, OfferPicture)] = {
    val offset = pageSize * page
    val query = (for {
      o <- offers.filter(_.userid === userId)
    } yield (
        o.id.?,
        o.title,
        o.description,
        o.price,
        o.locale,
        o.currency_code,
        o.shop,
        o.highlight,
        o.waggle,
        o.telephone.?,
        o.userid,
        o.created_on.?,
        o.updated_on.?)
      ).sortBy(_._11.desc).drop(offset).take(pageSize)
    val totalRows = offerCountOfUser(userId)
    val ofrs = query.list.map( row =>
        Offer(row._1, row._2, row._3, row._4, row._5, row._6, row._7, row._8, row._9, row._10, row._11, row._12, row._13)
    )
    val oids = ofrs.map(_.id.get)

    val status = (for {
      s <- listingStatuses.filter(_.offerid inSet oids)
    } yield
      s.offerid -> s.status
      ).toMap

    val pics = (for {
      p <- pictures.filter(_.offerid inSet oids)
    } yield
      (p.offerid, p.name)
      ).list.groupBy(_._1).toMap

    val resOffers = ofrs.map { o =>
      val picName = pics.getOrElse(o.id.get, List((0.toLong, "none"))).head._2
      if (picName.split("\\.").length > 1) (o, OfferStatus(status.getOrElse(o.id.get, "none")), OfferPicture( picName ))
      else (o, OfferStatus(status.getOrElse(o.id.get, "none")), OfferPicture( picName +".jpg" ))
    }
    Page(resOffers, page, offset, totalRows)
  }
  /**
   * Insert a new offer
   * @param offer
   */
  def insert(offer: Offer)(implicit session: Session): Unit =
    offers.insert(offer)
  /**
   * Insert a new offer returning the auto increment id as a Long
   * @param offer
   * @param session
   * @return
   */
  def insertReturningId(offer: Offer)(implicit session: Session): Long =
    offers returning offers.map(_.id) insert offer
  /**
   * Update a offer
   * @param id
   * @param offer
   */
  def update(id: Long, offer: Offer)(implicit session: Session): Unit = {
    val offerToUpdate: Offer = offer.copy(Some(id))
    offers.filter(_.id === id).update(offerToUpdate)
  }
  /**
   *
   * @param id
   * @param title
   * @return
   */
//  def offerById(id: Column[Int], title: Column[String]): Query[Offers, Offer] = {
//    for {
//      o <- offers if o.id == id
//    } yield o
//  }
  /**
   *
   * @param id
   * @param titleOpt
   * @param descriptionOpt
   * @param priceOpt
   * @param shopOpt
   * @param session
   * @return
   */
  def updateFields(id: Long,
                   titleOpt: Option[String],
                   descriptionOpt: Option[String],
                   priceOpt: Option[Double],
                   shopOpt: Option[String],
                   telephoneOpt: Option[String])(implicit  session:Session): Unit = {
    val query = offers.filter(_.id === id)
    titleOpt match {
      case Some(title) =>
        query.map { row =>
          row.title
        }.update(title)
      case None =>
    }
    descriptionOpt match {
      case Some(description) =>
        query.map { row =>
          row.description
        }.update(description)
      case None =>
    }
    priceOpt match {
      case Some(price) =>
        query.map { row =>
          row.price
        }.update(price)
      case None =>
    }
    shopOpt match {
      case Some(shop) =>
        query.map { row =>
          row.shop
        }.update(shop)
      case None =>
    }
    telephoneOpt match {
      case Some(telephone) =>
        query.map { row =>
          row.telephone
        }.update(telephone)
      case None =>
    }
  }

  def upgradeListing(offerid: Long, waggle: Boolean, highlight: Boolean)(implicit  session:Session): Unit = {
    val query = offers.filter(_.id === offerid)
    val now = new Timestamp(System.currentTimeMillis)
    if (waggle) {
        query.map { row =>
          (row.waggle, row.created_on)
        }.update(waggle, now)
    }
    if (highlight) {
        query.map { row =>
          (row.highlight, row.created_on)
        }.update(highlight, now)
    }
  }
  /**
   * Delete a offer
   * @param id
   */
  def delete(id: Long)(implicit session: Session): Unit =
    offers.filter(_.id === id).delete
}