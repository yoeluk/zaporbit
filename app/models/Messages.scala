package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import java.sql.Timestamp

case class ZOMessage(received_status: String = "unread",
                    message: String,
                    senderid: Long,
                    recipientid: Long,
                    created_on: Option[Timestamp] = None)

case class Message(id: Option[Long] = None,
                       received_status: String = "unread",
                       message: String,
                       convid: Long,
                       senderid: Long,
                       recipientid: Long,
                       created_on: Option[Timestamp] = None,
                       updated_on: Option[Timestamp] = None)

class Messages(tag: Tag) extends Table[Message](tag, "Messages") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def received_status = column[String]("received_status", O.NotNull)
  def message = column[String]("message", O.NotNull)
  def convid = column[Long]("convid", O.NotNull)
  def senderid = column[Long]("senderid", O.NotNull)
  def recipientid = column[Long]("recipientid", O.NotNull)
  def created_on = column[Timestamp]("created_on", O.Nullable)
  def updated_on = column[Timestamp]("updated_on", O.Nullable)
  def * = (id.?, received_status, message, convid, senderid, recipientid, created_on.?, updated_on.?) <> (Message.tupled, Message.unapply _)
  def conversation = foreignKey("fk_conversation_message", convid, TableQuery[Conversations])(_.id)
}

object Messages extends DAO {

  def insert(message: Message)(implicit session: Session): Unit =
    messages.insert(message)

  def delete(id: Long)(implicit session: Session): Unit =
    messages.filter(_.id === id).delete

  def findById(id: Long)(implicit session: Session): Option[Message] =
    messages.filter(_.id === id).firstOption

  def findByConvId(convId: Long)(implicit session: Session): Option[List[ZOMessage]] = {
    val q = for {
      m <- messages if m.convid === convId
    } yield (
        m.id.?,
        m.received_status,
        m.message,
        m.senderid,
        m.recipientid,
        m.created_on.?)
    Option(q.list.map { row =>
      ZOMessage(row._2, row._3, row._4, row._5, row._6)
    })
  }


  def findByTransId(id: Long)(implicit session: Session): Option[Message] =
    messages.filter(_.id === id).firstOption

  def insertReturningId(message: Message)(implicit session: Session): Option[Long] =
    Option(messages returning messages.map(_.id) insert message)

  def markAsReadByConvId(convid: Long)(implicit session: Session): Unit = {
    val q = messages.filter(_.convid === convid).filter(_.received_status like "%unread%")
    q.map { m =>
      m.received_status
    }.update("read")
  }

}