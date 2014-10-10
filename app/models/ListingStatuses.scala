package models

import java.sql.Timestamp
import play.api.db.slick.Config.driver.simple._

case class ListingStatus(id: Option[Long] = None,
                          status: String,
                          offerid: Long,
                          created_on: Option[Timestamp] = None,
                          updated_on: Option[Timestamp] = None)

class ListingStatuses(tag: Tag) extends Table[ListingStatus](tag, "ListingStatuses") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def status = column[String]("status", O.NotNull)
  def offerid = column[Long]("offerid", O.NotNull)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, status, offerid, created_on.?, updated_on.?) <> (ListingStatus.tupled, ListingStatus.unapply _)
  def offer = foreignKey("fk_status_offer", offerid, TableQuery[Offers])(_.id)
}


object ListingStatuses extends DAO {

  def findByStatus(status: String)(implicit session: Session): List[ListingStatus] =
    listingStatuses.filter(_.status === status).list
  /**
   * Insert a new status
   * @param status
   */
  def insert(status: ListingStatus)(implicit session: Session): Unit =
    listingStatuses.insert(status)

  def update(offerid: Long, status: String)(implicit  session: Session): Unit = {
    listingStatuses.filter(_.offerid === offerid).firstOption match {
      case Some(st) =>
        val query = listingStatuses.filter(_.offerid === offerid)
        query.map { row =>
          row.status
        }.update(status)
      case None =>
        val newListingStatus = ListingStatus(status = status, offerid = offerid)
        listingStatuses.insert(newListingStatus)
    }
  }

}