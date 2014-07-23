package models

/**
 * Created by yoelusa on 13/03/2014.
 */

import play.api.db.slick.Config.driver.simple._

case class Friend(id: Option[Long] = None,
                    userid: Long,
                    friendid: Long)

class Friends(tag: Tag) extends Table[Friend](tag, "Friends") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userid = column[Long]("userid", O.NotNull)
  def friendid = column[Long]("friendid", O.NotNull)
  def * = (id.?, userid, friendid) <> (Friend.tupled, Friend.unapply _)
  def user = foreignKey("fk_friend_user", userid, TableQuery[Users])(_.id)
}

object Friends extends DAO {

  /**
   *
   * @param friendid
   * @param session
   * @return
   */
  def findFollowersForFriend(friendid: Long)(implicit session: Session): List[User] =
    (for {
       l <- friends.filter(_.friendid === friendid)
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
  def insertAll(newFollowings: Seq[Friend])(implicit session: Session): Unit =
    friends.insertAll(newFollowings : _*)

  /**
   *
   * @param userid
   * @param newFollowings
   * @param session
   * @return
   */
  def updateFollowingForUser(userid: Long, newFollowings: List[Friend])(implicit session: Session) = {
    this.findFollowingForUser(userid) match {
      case Nil =>
        this.insertAll(newFollowings)
      case existingFriends =>
        val newFollowingsIds = newFollowings.map(f => f.friendid).toSet
        val existingFriendsIds = existingFriends.map(f => f.friendid).toVector
        val newFollowingList = for {
          fr <- newFollowings.filterNot(x => existingFriendsIds.contains(x.friendid))
        } yield fr
        this.insertAll(newFollowingList)
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
  def purgeUnfollowings(userid: Long, existingFriendsIds: Set[Long])(implicit session: Session) =
    (for {
      fl <- friends.filter(_.userid === userid)
        .filterNot(_.friendid inSet existingFriendsIds)
    } yield fl).delete


}