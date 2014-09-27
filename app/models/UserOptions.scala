package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._

case class UserOption(id: Option[Long] = None,
                      userid: Long,
                      about: Option[String] = None,
                      background: Option[String] = None,
                      picture: Option[String] = None)

class UserOptions(tag: Tag) extends Table[UserOption](tag, "UserOptions") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userid = column[Long]("userid", O.NotNull)
  def about = column[String]("about", O.Nullable)
  def background = column[String]("background", O.Nullable)
  def picture = column[String]("picture", O.Nullable)
  def * = (id.?, userid, about.?, background.?, picture.?) <> (UserOption.tupled, UserOption.unapply _)
}

object UserOptions extends DAO {

  /**
   *
   * @param userid
   * @param session
   * @return
   */
  def findByUserid(userid: Long)(implicit session: Session): Option[UserOption] =
    userOptions.filter(_.userid === userid).firstOption

  /**
   *
   * @param userOpts
   * @param session
   * @return
   */
  def update(userOpts: UserOption)(implicit session: Session) = {

    val q = userOptions.filter(_.userid === userOpts.userid)

    userOpts.about match {
      case Some(a) =>
        q.map(row => row.about).update(a)
      case None =>
    }
    userOpts.background match {
      case Some(b) =>
        println("updating background: " + b)
        q.map(row => row.background).update(b)
      case None =>
    }
    userOpts.picture match {
      case Some(p) =>
        q.map(row => row.picture).update(p)
      case None =>
    }

  }

  /**
   *
   * @param userOpt
   * @param session
   * @return
   */
  def insert(userOpt: UserOption)(implicit session: Session) =
    userOptions.insert(userOpt)

  /**
   *
   * @param userOpt
   * @param session
   * @return
   */
  def insertOrUpdate(userOpt: UserOption)(implicit session: Session) =
    this.findByUserid(userOpt.userid) match {
      case None => this.insert(userOpt)
      case Some(opts) => this.update(userOpt)
    }
}