package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

case class ZOConversation(id: Option[Long] = None,
                          user1_status: String,
                          user2_status: String,
                          user1id: Long,
                          user2id: Long,
                          title: String,
                          messages: List[ZOMessage],
                          offerid: Option[Long],
                          updated_on: Option[Timestamp] = None)

case class Conversation(id: Option[Long] = None,
                        user1_status: String,
                        user2_status: String,
                        user1id: Long,
                        user2id: Long,
                        title: String,
                        offerid: Option[Long] = None,
                        created_on: Option[Timestamp] = None,
                        updated_on: Option[Timestamp] = None)

class Conversations(tag: Tag) extends Table[Conversation](tag, "Conversations") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def user1_status = column[String]("user1_status", O.NotNull)
  def user2_status = column[String]("user2_status", O.NotNull)
  def user1id = column[Long]("user1id", O.NotNull)
  def user2id = column[Long]("user2id", O.NotNull)
  def title = column[String]("title", O.NotNull)
  def offerid = column[Long]("offerid", O.Nullable)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, user1_status, user2_status, user1id, user2id, title, offerid.?, created_on.?, updated_on.?) <> (Conversation.tupled, Conversation.unapply _)
}

object Conversations extends DAO {

  def insert(conversation: Conversation)(implicit session: Session): Unit =
    conversations.insert(conversation)

  def countByUser(userid: Long)(implicit session: Session): Long = {
    (for {
      c <- conversations if (c.user1id === userid && (c.user1_status.toLowerCase like "%online&")) ||
      (c.user2id === userid && (c.user2_status.toLowerCase like "%online%"))
    } yield c.updated_on.?).list.length
  }

  def update(id: Option[Long], convo: Conversation)(implicit session: Session): Unit = {
    val updatedConvo = convo.copy(id)
    conversations.filter(_.id === id).update(updatedConvo)
  }

  def delete(id: Long)(implicit session: Session): Unit =
    conversations.filter(_.id === id).delete

  def findById(id: Long)(implicit session: Session): Option[Conversation] =
    conversations.filter(_.id === id).firstOption

  def insertReturningId(conversation: Conversation)(implicit session: Session): Option[Long] =
    Option(conversations returning conversations.map(_.id) insert conversation)

  def convsForUser(page: Int = 0,
                  pageSize: Int = 20,
                  userid: Long)(implicit session: Session): Page[(ZOConversation, User, User)] = {
    val offset = pageSize * page
    val query = (for {
      c <- conversations if (c.user1id === userid && (c.user1_status.toLowerCase like "online")) ||
      (c.user2id === userid && (c.user2_status.toLowerCase like "online"))
    } yield (
        c.id.?,
        c.user1_status,
        c.user2_status,
        c.user1id,
        c.user2id,
        c.title,
        c.offerid.?,
        c.created_on.?,
        c.updated_on.?)
      ).sortBy(_._1.desc).drop(offset).take(pageSize)
    val totalRows = countByUser(userid)
    val result = query.list.map { row =>
      (ZOConversation(row._1, row._2, row._3, row._4, row._5, row._6,
        Messages.findByConvId(row._1.get).orNull,
        row._7, row._9),
        Users.findById(row._4).orNull,
        Users.findById(row._5).orNull
      )
    }
    Page(result, page, offset, totalRows)
  }

  def userLeaveConvo(convid: Long, userid: Long)(implicit session: Session): Unit = {
    findById(convid) match {
      case Some(convo) =>
        if (convo.user1_status == "online" && convo.user2_status == "online") {
          if (convo.user1id == userid) {
            val newConvo = Conversation(
              convo.id,
              "left",
              convo.user2_status,
              convo.user1id,
              convo.user2id,
              convo.title,
              convo.offerid)
            update(Some(convid), newConvo)
          } else {
            val newConvo = Conversation(
              convo.id,
              convo.user2_status,
              "left",
              convo.user1id,
              convo.user2id,
              convo.title,
              convo.offerid)
            update(Some(convid), newConvo)
          }
        } else {
          delete(convid)
        }
      case None =>
    }
  }

}