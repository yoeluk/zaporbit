package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._
import securesocial.core.OAuth2Info

case class OAuth2(id: Option[Long] = None,
                  fbuserid: Long,
                  accessToken: String,
                  tokenType: Option[String],
                  expiresIn: Option[Int],
                  refreshToken: Option[String])

class OAuth2s(tag: Tag) extends Table[OAuth2](tag, "OAuth2s") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def fbuserid = column[Long]("userid", O.NotNull)
  def accessToken = column[String]("accessToken", O.NotNull)
  def tokenType = column[String]("tokenType", O.Nullable)
  def expiresIn = column[Int]("expiresIn", O.Nullable)
  def refreshToken = column[String]("refreshToken", O.Nullable)
  def * = (id.?, fbuserid, accessToken, tokenType.?, expiresIn.?, refreshToken.?) <> (OAuth2.tupled, OAuth2.unapply _)
  def user = foreignKey("fk_OAuth2_user", fbuserid, TableQuery[Users])(_.fbuserid)
}

object OAuth2s extends DAO {
  /**
   * Retrieve a OAuth2 with userid
   * @param fbuserid
   */
  def findByUserId(fbuserid: Long)(implicit session: Session): Option[OAuth2Info] =
    oauth2info.filter(_.fbuserid === fbuserid).firstOption match {
      case Some(auth2) =>
        Some(OAuth2Info(auth2.accessToken, auth2.tokenType,auth2.expiresIn,auth2.refreshToken))
      case _ => None
    }
  /**
   *
   * @param fbuserid
   * @param session
   * @return
   */
  def deleteOAuth2(fbuserid: Long)(implicit session: Session) =
    oauth2info.filter(_.fbuserid === fbuserid).delete
  /**
   * Insert a new oauth2
   * @param oauth2
  *  @param fbuserid
   */
  def insertUpdate(oauth2: OAuth2Info, fbuserid: Long)(implicit session: Session) = {
    val newOAuth2 = OAuth2(
      id = None,
      fbuserid = fbuserid,
      accessToken = oauth2.accessToken,
      tokenType = oauth2.tokenType,
      expiresIn = oauth2.expiresIn,
      refreshToken = oauth2.refreshToken
    )
    oauth2info.filter(_.fbuserid === fbuserid).firstOption match {
      case Some(oauth2db) =>
        oauth2info.update(newOAuth2.copy(id = oauth2db.id))
      case None =>
        oauth2info.insert(newOAuth2)
    }
  }

}