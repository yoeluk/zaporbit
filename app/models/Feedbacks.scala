package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

case class Feedback(id: Option[Long] = None,
                    feedback: String,
                    userid: Long,
                    by_userid: Long,
                    transid: Long,
                    created_on: Option[Timestamp] = None,
                    updated_on: Option[Timestamp] = None)

class Feedbacks(tag: Tag) extends Table[Feedback](tag, "Feedbacks") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def feedback = column[String]("feedback", O.NotNull)
  def userid = column[Long]("userid", O.NotNull)
  def by_userid = column[Long]("by_userid", O.NotNull)
  def transid = column[Long]("transid", O.NotNull)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, feedback, userid, by_userid, transid, created_on.?, updated_on.?) <> (Feedback.tupled, Feedback.unapply _)
  def transaction = foreignKey("fk_transaction_feedback", userid, TableQuery[Transactions])(_.id)
}

object Feedbacks extends DAO {
  /**
   *
   * @param id
   * @param session
   * @return
   */
  def findById(id: Long)(implicit session: Session): Option[Feedback] =
    feedbacks.filter(_.id === id).firstOption

  def findForUser(userid: Long)(implicit session: Session): Option[List[Feedback]] =
    (for {
      f <- feedbacks.filter(_.userid === userid)
    } yield f).list match {
      case Nil => None
      case head::tail => Some(head::tail)
    }

  /**
   *
   * @param transid
   * @param by_userid
   * @param session
   * @return
   */
  def findByUserWithTransid(transid: Long, by_userid: Long)(implicit session: Session): Option[Feedback] =
    feedbacks.filter(_.transid === transid).filter(_.by_userid === by_userid).firstOption

  /**
   *
   * @param transids
   * @param by_userid
   * @param session
   * @return
   */
  def findCompletedFeedback(transids: List[Long], by_userid: Long)(implicit session: Session): List[(Feedback, Int)] =  {
    (for {
      r <- ratings.filter(_.transid inSet transids).filter(_.by_userid === by_userid)
      f <- r.feedback
    } yield (f, r.rating)).list
  }

  /**
   *
   * @param transids
   * @param by_userid
   * @param session
   * @return
   */
  def findFailedFeedback(transids: List[Long], by_userid: Long)(implicit session: Session): List[Feedback] =  {
    (for {
      f <- feedbacks.filter(_.transid inSet transids).filter(_.by_userid === by_userid)
    } yield f).list
  }

  /**
   * Find feedback for user with id
   * @param userid
   */
  def feedbacksForUserId(userid: Long)(implicit session: Session): List[String] = {
    (for {
      f <- feedbacks.filter(_.userid === userid)
    } yield f.feedback).list
  }
  /**
   * Insert a new feedback
   * @param feedback
   */
  def insert(feedback: Feedback)(implicit session: Session): Unit =
    feedbacks.insert(feedback)

  def insertReturningId(feedback: Feedback)(implicit session: Session): Long =
    feedbacks returning feedbacks.map(_.id) insert feedback

  /**
   * Delete a feedback with id
   * @param id
   */
  def delete(id: Long)(implicit session: Session): Unit =
    feedbacks.filter(_.id === id).delete
}