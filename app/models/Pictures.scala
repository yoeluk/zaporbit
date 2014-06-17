package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._

case class Picture(id: Option[Long] = None,
                   name: String,
                   offerid: Long)

class Pictures(tag: Tag) extends Table[Picture](tag, "Pictures") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def offerid = column[Long]("offerid", O.NotNull)
  def * = (id.?, name, offerid) <> (Picture.tupled, Picture.unapply _)
  def offer = foreignKey("fk_picture_offer", offerid, TableQuery[Offers])(_.id)
}

object Pictures extends DAO {
  /**
   * Retrieve a picture with id
   * @param id
   */
  def findById(id: Long)(implicit session: Session): Option[Picture] =
    pictures.filter(_.id === id).firstOption
  /**
   * Find pictures with offer id
   * @param offerid
   */
  def picturesByOfferId(offerid: Long)(implicit session: Session): List[String] = {
    (for {
      picture <- pictures.filter(_.offerid === offerid)
    } yield picture.name).list
  }

  def firstPicturesFromOffer(offerid: Long)(implicit session: Session): Option[String] =
    picturesByOfferId(offerid).headOption
  /**
   *
   * @param offerid
   * @param name
   * @param session
   * @return
   */
  def deletePictureByName(offerid: Long, name: String)(implicit session: Session) {
    val pictureOpt = Option((for {
      p <- pictures.filter(_.offerid === offerid)
      if p.name === name
    } yield p).first)
    pictureOpt match {
      case Some(picture) =>
        pictures.filter(_.id === picture.id).delete
      case None =>
    }
  }
  /**
   * Insert a new picture
   * @param picture
   */
  def insert(picture: Picture)(implicit session: Session): Unit =
    pictures.insert(picture)
  /**
   * Delete a picture with id
   * @param id
   */
  def delete(id: Long)(implicit session: Session): Unit =
    pictures.filter(_.id === id).delete
  /**
   * Delete a picture with name
   * @param name
   */
  def deleteByName(name: String)(implicit session: Session): Unit =
    pictures.filter(_.name === name).delete
}
