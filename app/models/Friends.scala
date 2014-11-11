package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._

case class Friend(id: Option[Long] = None,
                    userid: Long,
                    friendid: Long,
                    friendfbid: String)

class Friends(tag: Tag) extends Table[Friend](tag, "Friends") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userid = column[Long]("userid", O.NotNull)
  def friendid = column[Long]("friendid", O.NotNull)
  def friendfbid = column[String]("friendfbid", O.NotNull)
  def * = (id.?, userid, friendid, friendfbid) <> (Friend.tupled, Friend.unapply _)
  def user = foreignKey("fk_friend_user", userid, TableQuery[Users])(_.id)
}

object Friends extends DAO {

  /**
   *
   * @param friendfbid
   * @param session
   * @return
   */
  def findFollowersForFriend(friendfbid: String)(implicit session: Session): List[User] =
    (for {
       l <- friends.filter(_.friendfbid === friendfbid)
       f <- l.user
     } yield f).list

  /**
   *
   * @param userid
   * @param session
   * @return
   */
  def findFollowingForUser(userid: Long)(implicit session: Session): List[Friend] = {
    friends.filter(_.userid === userid).list
  }

  def isFollowing(userid: Long, friendid: Long)(implicit session: Session): Boolean = {
    friends.filter(_.userid === userid).filter(_.friendid === friendid).firstOption match {
      case None => false
      case Some(_) => true
    }

  }

  /**
   *
   * @param friend
   * @param session
   * @return
   */
  def insert(friend: Friend)(implicit session: Session): Unit =
    friends.insert(friend)

  /**
   *
   * @param newFollowings
   * @param session
   * @return
   */
  def insertAllNewFollowings(newFollowings: Seq[Option[Friend]])(implicit session: Session): Unit = {
    val nff = for {
      f <- newFollowings if f != None
    } yield f.get
    friends.insertAll(nff : _*)
  }

  def deleteFollowing(userid: Long, friendid: Long)(implicit session: Session) =
    friends.filter(x => x.userid === userid && x.friendid === friendid).delete

  /**
   *
   * @param userid
   * @param newFollowings
   * @param session
   * @return
   */
  def updateFollowingForUser(userid: Long, newFollowings: List[Option[Friend]])(implicit session: Session) = {
    this.findFollowingForUser(userid) match {
      case Nil =>
        this.insertAllNewFollowings(newFollowings)
      case existingFriends =>
        val nff = for {
          f <- newFollowings if f != None
        } yield f.get
        val newFollowingsIds = nff.map(f => f.friendfbid).toSet
        val existingFriendsIds = existingFriends.map(f => f.friendfbid).toVector
        val newFollowingList = for {
          fr <- nff.filterNot(x => existingFriendsIds.contains(x.friendfbid))
        } yield fr
        friends.insertAll(newFollowingList : _*)
        this.purgeUnfollowings(userid, newFollowingsIds)
    }
  }

  /**
   *
   * @param userid
   * @param existingFriendsIds
   * @param session
   * @return
   */
  def purgeUnfollowings(userid: Long, existingFriendsIds: Set[String])(implicit session: Session) =
    (for {
      fl <- friends.filter(_.userid === userid)
        .filterNot(_.friendfbid inSet existingFriendsIds)
    } yield fl).delete


}