package models

import securesocial.core._

import _root_.java.sql.Timestamp
import play.api.db.slick.Config.driver.simple._

/**
 * Created by yoelusa on 13/03/2014.
 */

case class User(id: Option[Long],
                name: String,
                surname: String,
                fbuserid: Long,
                email: String,
                isMerchant: Option[Boolean] = Some(false),
                created_on: Option[Timestamp] = None)

case class ExportedUser(id: Option[Long],
                        providerId: String,
                        userId: String,
                        firstName: Option[String],
                        lastName: Option[String],
                        fullName: Option[String],
                        email: Option[String],
                        avatarUrl: Option[String],
                        authMethod: AuthenticationMethod,
                        oAuth1Info: Option[OAuth1Info],
                        oAuth2Info: Option[OAuth2Info],
                        passwordInfo: Option[PasswordInfo]) extends GenericProfile

class Users(tag: Tag) extends Table[User](tag, "Users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def surname = column[String]("surname", O.NotNull)
  def fbuserid = column[Long]("fbuserid", O.NotNull)
  def email = column[String]("email", O.NotNull)
  def isMerchant = column[Boolean]("isMerchant", O.Nullable)
  def created_on =  column[Timestamp]("created_on", O.NotNull)
  def * = (id.?, name, surname, fbuserid, email, isMerchant.?, created_on.?) <> (User.tupled, User.unapply _)
}

object Users extends DAO {

  def findByFbId(fbuserid: Long)(implicit session: Session): Option[User] =
    users.filter(_.fbuserid === fbuserid).firstOption

  def findById(id: Long)(implicit session: Session): Option[User] =
    users.filter(_.id === id).firstOption

  def insert(user: User)(implicit s: Session): Unit =
    users.insert(user)

  def insertReturningId(user: User)(implicit session: Session): Long =
    users returning users.map(_.id) insert user

  def update(id: Option[Long], isMerchant: Option[Boolean], user: User)(implicit session: Session) = {
    val updatedUser = user.copy(id = id, isMerchant = isMerchant)
    users.filter(_.id === id).update(updatedUser)
  }

  def updateMerchant(isMerchant: Boolean, userid: Long)(implicit session: Session) =
    users.filter(_.id === userid).map { row =>
      row.isMerchant
    }.update(isMerchant)

}
