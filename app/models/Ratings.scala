package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

case class Rating(id: Option[Long] = None,
                    rating: Int,
                    userid: Long,
                    by_userid: Long,
                    transid: Long,
                    feedbackid: Long,
                    created_on: Option[Timestamp] = None,
                    updated_on: Option[Timestamp] = None)

class Ratings(tag: Tag) extends Table[Rating](tag, "Ratings") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def rating = column[Int]("rating", O.NotNull)
  def userid = column[Long]("userid", O.NotNull)
  def by_userid = column[Long]("by_userid", O.NotNull)
  def transid = column[Long]("transid", O.NotNull)
  def feedbackid = column[Long]("feedbackid", O.NotNull)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, rating, userid, by_userid, transid, feedbackid, created_on.?, updated_on.?) <> (Rating.tupled, Rating.unapply _)
  def feedback = foreignKey("fk_feedback_rating", feedbackid, TableQuery[Feedbacks])(_.id)
}

object Ratings extends DAO {
  /**
   * Retrieve a rating with id
   * @param id
   */
  def findById(id: Long)(implicit session: Session): Option[Rating] =
    ratings.filter(_.id === id).firstOption
  /**
   * Find rating for user with id
   * @param userid
   */
  def ratingsForUserId(userid: Long)(implicit session: Session): Option[List[Int]] = {
    (for {
      r <- ratings.filter(_.userid === userid)
    } yield r.rating).list match {
      case Nil => None
      case l => Some(l)
    }
  }

  /**
   *
   * @param userid
   * @param session
   * @return
   */
  def ratingForUser(userid: Long)(implicit session: Session): (Float, Int) = {
    ratingsForUserId(userid) match {
      case Some(ratingList) =>
        ((ratingList.sum.toFloat+50)/(5*(ratingList.length+10)), ratingList.length)
      case None => (1,0)
    }
  }

  /**
   * Insert a new rating
   * @param rating
   */
  def insert(rating: Rating)(implicit session: Session): Unit =
    ratings.insert(rating)
  /**
   * Delete a rating with id
   * @param id
   */
  def delete(id: Long)(implicit session: Session): Unit =
    ratings.filter(_.id === id).delete
}