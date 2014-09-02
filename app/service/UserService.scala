package service

import play.api.Logger
import securesocial.core._
import securesocial.core.providers.MailToken
import scala.concurrent.Future
import securesocial.core.services.{UserService, SaveMode}
import play.api.db.slick._
import play.api.Play.current

import models._

// a simple User class that can have multiple identities
//case class ExtUser(main: BasicIdentity, identities: List[BasicIdentity])

object UserFromIdentity {
  def apply(b: BasicProfile)(implicit session: Session): User =
    User(None,
      b.firstName.getOrElse("no name"),
      b.lastName.getOrElse("no surname"),
      b.userId.toLong,
      b.email.orNull,
      Users.findByFbId(b.userId.toLong) match {
        case Some(user) => user.isMerchant
        case None => Some(false)
      })
}

object ExportedUserFromUser {
  def apply(u: User, providerId: String, userId: String)(implicit session: Session): BasicProfile =
    BasicProfile(
      providerId,
      userId,
      Some(u.name),
      Some(u.surname),
      Some(u.name +" "+ u.surname),
      Some(u.email),
      None,
      AuthenticationMethod.OAuth2,
      None,
      OAuth2s.findByUserId(userId.toLong),
      None
    )
}

case class SocialUser(main: User, identities: List[User])

class SocialUserService extends UserService[SocialUser] {
  val logger = Logger("application.controllers.SocialUserService")

  //var users = Map[(String, String), SocialUser]()
  /**
   * Finds a user that maches the specified id
   *
   * @param providerId the user id
   * @return an optional user
   */

  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
    DB.withSession { implicit s =>
      Future.successful(
        Users.findByFbId(userId.toLong) match {
        case Some(user) =>
          Some(ExportedUserFromUser(user, providerId, userId))
        case _ => None
        }
      )
    }
  }

  /**
   * Saves the user.  This method gets called when a user logs in.
   * This is your chance to save the user information in your backing store.
   *
   * @param basicUser basic identity
   * @return Exported User
   */
  def save(basicUser: BasicProfile, mode: SaveMode): Future[SocialUser] = {
    DB.withSession { implicit s =>
      Future.successful(
        Users.findByFbId(basicUser.userId.toLong) match {
          case None =>
            val preUser = UserFromIdentity(basicUser)
            val id = Option(Users.insertReturningId(preUser))
            val user = preUser.copy(id = id)
            OAuth2s.insertUpdate(basicUser.oAuth2Info.get, user.fbuserid)
            SocialUser(main = user, identities = Nil)
          case Some(existingUser) =>
            val persistantUser: User = UserFromIdentity(basicUser)
            Users.update(existingUser.id, persistantUser.isMerchant, persistantUser)
            val user = persistantUser.copy(id = existingUser.id)
            OAuth2s.insertUpdate(basicUser.oAuth2Info.get, user.fbuserid)
            SocialUser(main = user, identities = Nil)
        }
      )
    }
  }

  def updatePasswordInfo(user: SocialUser, info: PasswordInfo): Future[Option[BasicProfile]] = {
    Future.successful {
      None
    }
  }

  def passwordInfoFor(user: SocialUser): Future[Option[PasswordInfo]] = {
    Future.successful {
      Some(PasswordInfo("123", "123", None))
    }
  }

  /**
   *
   * @param current current user
   * @param to link to user
   * @return
   */
  def link(current: SocialUser, to: BasicProfile): Future[SocialUser] = {
    Future.successful(current)
  }

  /**
   * Finds a user by email and provider id.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation.
   *
   * @param email - the user email
   * @param providerId - the provider id
   * @return
   */
  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
    Future.successful(None)
  }

  /**
   * Saves a token.  This is needed for users that
   * are creating an account in the system instead of using one in a 3rd party system.
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token The token to save
   */
  def saveToken(token: MailToken): Future[MailToken] =
    Future.successful(token)


  /**
   * Finds a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token the token id
   * @return
   */
  def findToken(token: String): Future[Option[MailToken]] = {
    Future.successful(None)
  }


  /**
   * Deletes a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token id
   */
  def deleteToken(uuid: String): Future[Option[MailToken]] = {
    // implement me
    Future.successful(None)
  }

  /**
   * Deletes all expired tokens
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   */
  def deleteExpiredTokens() {
    // implement me
  }

}