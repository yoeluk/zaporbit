/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package service

import play.api.Logger
import securesocial.core._
import securesocial.core.providers.FacebookProvider
import scala.concurrent.Future
import play.api.Application
import securesocial.core.{Identity, IdentityId, UserServicePlugin}
import securesocial.core.providers.Token
import play.api.db.slick._
import play.api.Play.current

import models._

// a simple User class that can have multiple identities
//case class ExtUser(main: BasicIdentity, identities: List[BasicIdentity])

object UserFromIdentity {
    def apply(i: Identity)(implicit session: Session): User = User(None,
      i.firstName,
      i.lastName,
      i.identityId.userId.toLong,
      i.email.orNull,
      Users.findByFbId(i.identityId.userId.toLong) match {
        case Some(user) => user.isMerchant
        case None => Some(false)
      })
}

object ExportedUserFromIdentity {
  def apply(i: Identity, id: Option[Long]): ExportedUser = ExportedUser(id, i.identityId, i.firstName, i.lastName, i.fullName, i.email,
    i.avatarUrl,i.authMethod,i.oAuth1Info,i.oAuth2Info,i.passwordInfo)
}

class UserService (application: Application) extends UserServicePlugin(application) {
  val logger = Logger("application.controllers.UserService")

  /**
   * Finds a user that maches the specified id
   *
   * @param id the user id
   * @return an optional user
   */
  def find(id: IdentityId): Option[ExportedUser] = {
    DB.withSession { implicit s =>
      Users.findByFbId(id.userId.toLong) match {
        case Some(user) =>
          Option(ExportedUser(user.id,new IdentityId(user.fbuserid.toString, "facebook"),
          user.name,user.surname,user.name+" "+user.surname,Option(user.email),None,new AuthenticationMethod("oauth2"),None,None,None))
        case None => None
      }
    }
  }

  /**
   * Saves the user.  This method gets called when a user logs in.
   * This is your chance to save the user information in your backing store.
   * @param user
   */
  def save(user: Identity): Identity = {
    DB.withSession { implicit s =>
      this.find(user.identityId) match {
        case None =>
          val id = Option(Users.insertReturningId(UserFromIdentity(user)))
          ExportedUserFromIdentity(user, id)
        case Some(existingUser) =>
          val persistantUser: User = UserFromIdentity(user)
          Users.update(existingUser.id, persistantUser.isMerchant, persistantUser)
          ExportedUserFromIdentity(user, existingUser.id)
      }
    }
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
  def findByEmailAndProvider(email: String, providerId: String):Option[Identity] = {
    None
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
  def save(token: Token) = {
    // implement me
  }


  /**
   * Finds a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param token the token id
   * @return
   */
  def findToken(token: String): Option[Token] = {
    None
  }


  /**
   * Deletes a token
   *
   * Note: If you do not plan to use the UsernamePassword provider just provide en empty
   * implementation
   *
   * @param uuid the token id
   */
  def deleteToken(uuid: String) {
    // implement me
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