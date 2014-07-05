package controllers

import play.api._
import play.api.mvc._
import play.api.Play._
import play.api.libs.json._
import play.api.db.slick._
import play.api.Play.current

import play.api.data._
import play.api.data.Forms._
import play.api.data.format._
import play.api.data.format.Formats._

import org.apache.commons.codec.binary.Base64
import com.typesafe.plugin._

import _root_.java.sql.Timestamp
import play.api.libs.functional.syntax._
import java.io.File

import models._
import views._

import org.cryptonode.jncryptor._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import securesocial.core._

/**
 * Created by yoelusa on 13/03/2014.
 */

object EmailService {
  //import play.api.i18n.Messages
  def sendWelcomeEmail(contact: User) = {
    val mail = use[MailerPlugin].email
    mail.setSubject("Welcome to ZapOrbit")
    mail.setFrom("support@zaporbit.com")
    mail.setRecipient(contact.email)
    val body = views.html.welcomeEmail.render(contact).body;
    mail.sendHtml(body)
  }
}

/*
 * reference from play-framework group
 * https://groups.google.com/forum/#!topic/play-framework/X9u0WREHg9E
 */
object FormMappings {
  implicit val sqlTimestampFormat: Formatter[Timestamp] = sqlTimestampFormat("yyyy-MM-dd HH:mm:ss")
  def sqlTimestampFormat(pattern: String): Formatter[Timestamp] = new Formatter[Timestamp] {
    override val format = Some("format.date", Seq(pattern))
    def bind(key: String, data: Map[String, String]) = {
      dateFormat(pattern).bind(key, data).right.map(d => new Timestamp(d.getTime))
    }
    def unbind(key: String, value: Timestamp) = dateFormat(pattern).unbind(key, value)
  }
  def sqlTimestamp(pattern: String): Mapping[Timestamp] = of[Timestamp] as sqlTimestampFormat(pattern)

}

object AppCryptor {
  val appCryptor = new AES256JNCryptor
  val password = "bjmlBqAfiBEQ4oZfaGtI0oMcd5IGkCp"
}

object API extends Controller {
  import  AppCryptor._
  import FormMappings._

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global
  /*
   * reference from accepted answer in stackoverlow
   * http://stackoverflow.com/questions/17281291/how-do-i-write-a-json-format-for-an-object-in-the-java-library-that-doesnt-have
   */
  implicit val rdsTimestamp: Reads[Timestamp] = (__ \ "time").read[Long].map{ long => new Timestamp(long) }
  implicit val wrsTimestamp: Writes[Timestamp] = (__ \ "time").write[String].contramap{ (a: Timestamp) => a.toString }
  implicit val fmtTimestamp: Format[Timestamp] = Format(rdsTimestamp, wrsTimestamp)

  implicit val rdsIdentityId: Reads[IdentityId] = ((__ \ "userId").read[String] and (__ \ "providerId").read[String]).tupled.map { case (userId,providerId) => new IdentityId(userId,providerId)}
  implicit val wrsIdentityId: Writes[IdentityId] = ((__ \ "userId").write[String] and (__ \ "providerId").write[String]).tupled.contramap { (a: IdentityId) => (a.userId,a.providerId) }
  implicit val fmtIdentityId: Format[IdentityId] = Format(rdsIdentityId, wrsIdentityId)

  // IMPLICIT JSON WRITES
  implicit val implicitListingZOConverstionWrites = new Writes[Page[(ZOConversation, User, User)]] {
    def writes(page: Page[(ZOConversation, User, User)]): JsValue = {
      JsArray(
        for {
          item <- page.items
        } yield Json.obj(
          "conversation" -> Json.obj(
            "id" -> item._1.id,
            "user1_status" -> item._1.user1_status,
            "user2_status" -> item._1.user2_status,
            "user1id" -> item._1.user1id,
            "user2id" -> item._1.user2id,
            "title" -> item._1.title,
            "messages" -> (for {
              m <- item._1.messages
            } yield Json.obj(
                "received_status" -> m.received_status,
                "message" -> m.message,
                "senderid" -> m.senderid,
                "recipientid" -> m.recipientid,
                "created_on" -> m.created_on.orNull.toString)),
            "updated_on" -> item._1.updated_on.orNull.toString
          ),
          "user1" -> Json.obj(
            "id" -> item._2.id,
            "name" -> item._2.name,
            "surname" -> item._2.surname,
            "fbuserid" -> item._2.fbuserid
          ),
          "user2" -> Json.obj(
            "id" -> item._3.id,
            "name" -> item._3.name,
            "surname" -> item._3.surname,
            "fbuserid" -> item._3.fbuserid
          )
        )
      )
    }
  }
  implicit val implicitListingZOLocationWrites = new Writes[Page[(Listing, ZOLocation, User)]] {
    def writes(page: Page[(Listing, ZOLocation, User)]): JsValue = {
      JsArray(
        for {
          item <- page.items
        } yield Json.obj(
          "listing" -> Json.obj(
            "id" -> item._1.id,
            "title" -> item._1.title,
            "description" -> item._1.description,
            "price" -> item._1.price,
            "locale" -> item._1.locale,
            "pictures" -> item._1.pictures,
            "shop" -> item._1.shop,
            "highlight" -> item._1.highlight,
            "waggle" -> item._1.waggle,
            "telephone" -> item._1.telephone,
            "userid" -> item._1.userid,
            "created_on" -> item._1.created_on.orNull.toString,
            "updated_on" -> item._1.updated_on.orNull.toString
          ),
          "location" -> Json.obj(
            "street" -> item._2.street,
            "locality" -> item._2.locality,
            "latitude" -> item._2.latitude,
            "longitude" -> item._2.longitude
          ),
          "user" -> Json.obj(
            "id" -> item._3.id,
            "name" -> item._3.name,
            "surname" -> item._3.surname,
            "fbuserid" -> item._3.fbuserid,
            "isMerchant" -> item._3.isMerchant,
            "email" -> item._3.email,
            "created_on" -> item._3.created_on
          )
        )
      )
    }
  }
  implicit val implicitListingUserWrites = new Writes[Page[(Listing, User)]] {
    def writes(page: Page[(Listing, User)]): JsValue = {
      JsArray(
        for {
          item <- page.items
        } yield Json.obj(
          "listing" -> Json.obj(
            "id" -> item._1.id,
            "title" -> item._1.title,
            "description" -> item._1.description,
            "price" -> item._1.price,
            "locale" -> item._1.locale,
            "pictures" -> item._1.pictures,
            "shop" -> item._1.shop,
            "highlight" -> item._1.highlight,
            "waggle" -> item._1.waggle,
            "userid" -> item._1.userid,
            "created_on" -> item._1.created_on.orNull.toString,
            "updated_on" -> item._1.updated_on.orNull.toString
          ),
          "user" -> Json.obj(
            "id" -> item._2.id,
            "title" -> item._2.name,
            "fb_Id" -> item._2.fbuserid
          )
        )
      )
    }
  }
  implicit val implicitOffersWrites = new Writes[Page[Listing]] {
    def writes(page: Page[Listing]): JsValue = {
      JsArray(
        for {
          item <- page.items
        } yield Json.obj(
          "id" -> item.id,
          "title" -> item.title,
          "description" -> item.description,
          "price" -> item.price,
          "locale" -> item.locale,
          "userid" -> item.userid,
          "pictures" -> item.pictures.orNull,
          "highlight" -> item.highlight,
          "waggle" -> item.waggle,
          "shop" -> item.shop,
          "updated_on" -> item.updated_on.orNull.toString
        )
      )
    }
  }
  implicit val implicitBuyingTransWrites = new Writes[Page[BuyingTrans]] {
    def writes(page: Page[BuyingTrans]): JsValue = {
      JsArray(
        for {
          item <- page.items
        } yield Json.obj(
          "id" -> item.id,
          "status" -> item.transStatus,
          "transid" -> item.transactionid,
          "title" -> item.offer_title,
          "description" -> item.offer_description,
          "price" -> item.offer_price,
          "sellerid" -> item.sellerid,
          "offerid" -> item.offerid,
          "updated_on" -> item.updated_on.orNull.toString
        )
      )
    }
  }
  implicit val implicitSellingTransWrites = new Writes[Page[SellingTrans]] {
    def writes(page: Page[SellingTrans]): JsValue = {
      JsArray(
        for {
          item <- page.items
        } yield Json.obj(
          "id" -> item.id,
          "status" -> item.transStatus,
          "transid" -> item.transactionid,
          "title" -> item.offer_title,
          "description" -> item.offer_description,
          "price" -> item.offer_price,
          "buyerid" -> item.buyerid,
          "offerid" -> item.offerid,
          "updated_on" -> item.updated_on.orNull.toString
        )
      )
    }
  }
  implicit val implicitBillingTransWrites = new Writes[Page[Billing]] {
    def writes(page: Page[Billing]): JsValue = {
      JsArray(
        for {
          item <- page.items
        } yield Json.obj(
          "id" -> item.id,
          "status" -> item.status,
          "title" -> item.offer_title,
          "description" -> item.offer_description,
          "price" -> item.offer_price,
          "userid" -> item.userid,
          "created_on" -> item.created_on.orNull.toString
        )
      )
    }
  }
  implicit val implicitGetRatingsWrites = new Writes[(Float,Int)] {
    def writes(myTuple: (Float,Int)): JsValue = {
      Json.obj(
        "rating" -> myTuple._1.toString,
        "total_ratings" -> myTuple._2
      )
    }
  }

  /**** Picture validation ****/

  implicit val pictureFormat = Json.format[Picture]
  val pictureForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "name" -> nonEmptyText,
      "offerid" -> longNumber
    )(Picture.apply)(Picture.unapply)
  )

  /**** Offer validation ****/

  implicit val offerFormat = Json.format[Offer]
  val offerForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "title" -> nonEmptyText,
      "description" -> nonEmptyText,
      "price" -> of[Double],
      "locale" -> nonEmptyText,
      "currency_code" -> nonEmptyText,
      "shop" -> nonEmptyText,
      "highlight" -> boolean,
      "waggle" -> boolean,
      "telephone" -> optional(nonEmptyText),
      "userid" -> longNumber,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Offer.apply)(Offer.unapply)
  )

  /**** Listing validation ****/

  implicit val listingFormat = Json.format[Listing]
  val listingForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "title" -> nonEmptyText,
      "description" -> nonEmptyText,
      "price" -> of[Double],
      "locale" -> nonEmptyText,
      "pictures" -> optional(list(nonEmptyText)),
      "shop" -> nonEmptyText,
      "highlight" -> boolean,
      "waggle" -> boolean,
      "telephone" -> optional(nonEmptyText),
      "userid" -> longNumber,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Listing.apply)(Listing.unapply)
  )

  /**** User validation ****/

  implicit val userFormat = Json.format[User]
  val userForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "name" -> nonEmptyText,
      "surname" -> nonEmptyText,
      "fbuserid" -> longNumber,
      "email" -> nonEmptyText,
      "isMerchant" -> optional(boolean),
      "created_on" -> optional(of[Timestamp])
    )(User.apply)(User.unapply)
  )

  /**** ZOLocation validation ****/

  implicit val zoLocationFormat = Json.format[ZOLocation]
  val zoLocationForm = Form(
    mapping(
      "street" -> nonEmptyText,
      "locality" -> nonEmptyText,
      "administrativeArea" -> nonEmptyText,
      "latitude" -> of[Double],
      "longitude" -> of[Double]
    )(ZOLocation.apply)(ZOLocation.unapply)
  )

  /**** Location validation ****/

  implicit val locationFormat = Json.format[Location]
  val locationForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "street" -> nonEmptyText,
      "locality" -> nonEmptyText,
      "administrativeArea" -> nonEmptyText,
      "latitude" -> of[Double],
      "longitude" -> of[Double],
      "offerid" -> optional(longNumber),
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Location.apply)(Location.unapply)
  )

  /**** Transaction validation ****/

  implicit val transactionFormat = Json.format[Transaction]
  val transactionForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "status" -> nonEmptyText,
      "offer_title" -> nonEmptyText,
      "offer_description" -> nonEmptyText,
      "offer_price" -> of[Double],
      "currency_code" -> nonEmptyText,
      "locale" -> nonEmptyText,
      "buyerid" -> longNumber,
      "sellerid" -> longNumber,
      "offerid" -> longNumber,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Transaction.apply)(Transaction.unapply)
  )

  /**** Buying validation ****/

  implicit val buyingFormat = Json.format[Buying]
  val buyingForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "status" -> nonEmptyText,
      "userid" -> longNumber,
      "transactionid" -> longNumber,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Buying.apply)(Buying.unapply)
  )

  /**** Selling validation ****/

  implicit val sellingFormat = Json.format[Selling]
  val sellingForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "status" -> nonEmptyText,
      "userid" -> longNumber,
      "transactionid" -> longNumber,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Selling.apply)(Selling.unapply)
  )

  /**** Selling validation ****/

  implicit val merchantFormat = Json.format[Merchant]
  val merchantForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "userid" -> longNumber,
      "identifier" -> nonEmptyText,
      "secret" -> nonEmptyText,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Merchant.apply)(Merchant.unapply)
  )

  /**** Billing validation ****/
  /*
  implicit val billingFormat = Json.format[Billing]
  val billingForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "status" -> nonEmptyText,
      "userid" -> longNumber,
      "offer_title" -> nonEmptyText,
      "offer_description" -> nonEmptyText,
      "offer_price" -> of[Double],
      "currency_code" -> nonEmptyText,
      "locale" -> nonEmptyText,
      "transactionid" -> longNumber,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Billing.apply)(Billing.unapply)
  )
  */
  implicit val implicitBillingWrites = new Writes[Billing] {
    def writes(bill: Billing): JsValue = {
      Json.obj(
        "id" -> bill.id.get,
        "status" -> bill.status,
        "userid" -> bill.userid,
        "offer_title" -> bill.offer_title,
        "offer_description" -> bill.offer_description,
        "offer_price" -> bill.offer_price,
        "locale" -> bill.locale,
        "transactionid" -> bill.transactionid,
        "wallet_order_id" -> bill.googlewallet_id.orNull,
        "created_on" -> bill.updated_on.orNull.toString
      )
    }
  }
  implicit val implicitBillingMapWrites = new Writes[Map[String,List[Billing]]] {
    def writes(bill: Map[String,List[Billing]]): JsValue = {
      Json.obj(
        "unpaid" -> JsArray(
          bill.get("unpaid") match {
            case Some(unpaidBilling) =>
              for {
                b <- unpaidBilling
              } yield {
                Json.toJson(b)
              }
            case None =>
              Nil
          }
        ),
        "paid" -> JsArray(
          bill.get("paid") match {
            case Some(paidBilling) =>
              for {
                b <- paidBilling
              } yield {
                Json.toJson(b)
              }
            case None =>
              Nil
          }
        )
      )
    }
  }

  /**** Conversation validation ****/

  implicit val conversationFormat = Json.format[Conversation]
  val conversationForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "user1_status" -> nonEmptyText,
      "user2_status" -> nonEmptyText,
      "user1id" -> longNumber,
      "user2id" -> longNumber,
      "title" -> nonEmptyText,
      "offerid" -> optional(longNumber),
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Conversation.apply)(Conversation.unapply)
  )

  /**** Message validation ****/

  implicit val messageFormat = Json.format[Message]
  val messageForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "received_status" -> nonEmptyText,
      "message" -> nonEmptyText,
      "convid" -> longNumber,
      "senderid" -> longNumber,
      "recipientid" -> longNumber,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Message.apply)(Message.unapply)
  )

  /**** Friend validation ****/

  implicit val friendFormat = Json.format[Friend]
  val friendForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "userid" -> longNumber,
      "friendid" -> longNumber
    )(Friend.apply)(Friend.unapply)
  )

  /**** Feedback validation ****/

  implicit val feedbackFormat = Json.format[Feedback]
  val feedbackForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "feedback" -> nonEmptyText,
      "userid" -> longNumber,
      "by_userid" -> longNumber,
      "transid" -> longNumber,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Feedback.apply)(Feedback.unapply)
  )
  implicit val implicitGetFeedbackWrites = new Writes[(Feedback,Int)] {
    def writes(myTuple: (Feedback,Int)): JsValue = {
      Json.obj(
        "feedback" -> Json.toJson(myTuple._1),
        "rating" -> myTuple._2
      )
    }
  }

  /**** Rating validation ****/

  implicit val ratingFormat = Json.format[Rating]
  val ratingForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "rating" -> number,
      "userid" -> longNumber,
      "by_userid" -> longNumber,
      "transid" -> longNumber,
      "feedbackid" -> longNumber,
      "created_on" -> optional(of[Timestamp]),
      "updated_on" -> optional(of[Timestamp])
    )(Rating.apply)(Rating.unapply)
  )

  /**************************/
  /**** GET REQUESTS API ****/
  /**************************/

  def listingsByLoc(page: Int, radius: Int) = DBAction(parse.json(maxLength = 1024)) {
    implicit rs =>
      (rs.request.body \ "location").validate[ZOLocation].map { loc =>
        val userid = rs.request.queryString.get("id").flatMap(_.headOption).getOrElse("0").toLong
        val pageResult = Locations.listByLoc(page = page, radius = radius, loc = loc, userid = userid)
        Ok(Json.toJson(pageResult))
      }.getOrElse(BadRequest(Json.obj(
        "status" -> "KO",
        "message" -> "invalid location"))
        )
  }

  def filterLocation(page: Int) = DBAction(parse.json(maxLength = 1024)) { implicit rs =>
    (rs.request.body \ "location").validate[ZOLocation].map { loc =>
      val filter = rs.request.queryString.get("filter").get(0)
      val userid = rs.request.queryString.get("id").flatMap(_.headOption).getOrElse("0").toLong
      val pageResult = Locations.filterLoc(page = page, loc = loc, filterStr = filter, userid = userid)
      Ok(Json.toJson(pageResult))
    }.getOrElse(BadRequest(Json.obj(
      "status" -> "KO",
      "message" -> "invalid location"
    )))
  }

  /**
   *
   * @param page
   * @param orderBy
   * @param userId
   * @return
   */
  def listingsByUsers(page: Int, orderBy: Int, userId: Long) = DBAction {
    implicit rs =>
      val pageResult = Offers.list1(page = page, orderBy = orderBy, userId = userId)
      Ok(Json.toJson(pageResult))
  }

  /**
   *
   * @param picture
   * @return
   */
  def downloadPicture(picture: String) = Action {
    Ok.sendFile(new File(configuration.getString("pictures_dir").get + picture + ".jpg"))
  }

  /**** PUT/POST REQUESTS API ****/

  /**
   *
   * @param name
   * @return
   */
  def savePictureToDisk(name: String) = Action(parse.file(to = new File(configuration.getString("pictures_dir").get + name + ".jpg")) ) {
    implicit request =>
      Ok(Json.obj(
        "status" -> "OK",
        "message" -> JsString("picture with name " + name + " was uploaded.")
        )
      )
  }
  /**
   * inserts a new listing item (offer + pictures)
   * @return
   */
  def receiveListing = DBAction(parse.json(maxLength = 1024)) { implicit rs =>
    (rs.request.body \ "location").validate[Location].map { location =>
      rs.request.body.validate[Offer].map { pseudOffer =>
        val insertedOfferId = Offers.insertReturningId(pseudOffer)
        for {
          (name, i) <- (rs.request.body \ "pictures").as[Seq[JsString]].zipWithIndex if i < 6
        } yield Json.obj(
          "name" -> name.as[String],
          "offerid" -> insertedOfferId
        ).validate[Picture].map { picture =>
          Pictures.insert(picture)
        }
        val newLocation = location.copy(offerid = Some(insertedOfferId))
        Locations.insert(newLocation)
        Ok(Json.obj(
          "status" -> "OK",
          "listingid" -> JsNumber(insertedOfferId)
        )
      )}.getOrElse(BadRequest(Json.obj(
        "status" -> "KO",
        "message" -> "invalid listing json")))
    }.getOrElse(BadRequest(Json.obj(
      "status" -> "KO",
      "message" -> "invalid location")))
  }
  /**
   * this method is called every time a user login (it inserts user if doesn't exists else updates user)
   * @return
   */
  def verifyingUser(tick: String) = DBAction(parse.raw) { implicit rs =>
    rs.request.body.asBytes(maxLength = 1024) match {
      case Some(body) =>
        val pass =  password+tick
        val decryptedBody = appCryptor.decryptData(body, pass.toCharArray)
        Json.parse(decryptedBody).validate[User].map { user =>
          Users.findByFbId(user.fbuserid) match {
            case Some(oldUser) =>
              if (oldUser.id.get == 21437) {
                EmailService.sendWelcomeEmail(oldUser)
              }
              val newUser: User = oldUser.copy(isMerchant = oldUser.isMerchant)
              Users.update(oldUser.id, oldUser.isMerchant, newUser)
              val rating = Ratings.ratingForUser(oldUser.id.get)
              Ok(Json.obj(
                "status" -> "OK",
                "message" -> JsString("user " + user.name +
                  " with old email " + oldUser.email +
                  " has been updated."),
                "userid" -> JsString("" + oldUser.id.get),
                "rating" -> rating
              ))
            case None =>
              val newUser: User = user.copy(isMerchant = Option(false))
              val id = Users.insertReturningId(newUser)
              //EmailService.sendWelcomeEmail(newUser)
              Ok(Json.obj(
                "status" -> "OK",
                "userid" -> JsString("" + id),
                "rating" -> Json.obj(
                  "rating" -> 1,
                  "total_ratings" -> 0)
              ))
          }
        }.getOrElse(BadRequest(Json.obj(
          "status" -> "OK",
          "message" -> "invalid user json")))
      case None =>
        BadRequest("invalid request")
    }
  }

  /**
   *
   * @param id
   * @return
   */
  def deleteListing(id: Long) = DBAction(parse.json) { implicit rs =>
    Transactions.findPendingOrProcessingTransById(id) match {
      case Some(trans) =>
        Forbidden(Json.obj(
          "status" -> "KO",
          "message" -> "Forbidden to modify a pending or processing transaction."
        ))
      case None =>
        val pictures = rs.request.body.as[List[String]]
        pictures.foreach { p =>
          val file = new File(configuration.getString("pictures_dir").get + p + ".jpg")
          if (file.exists) file.delete
        }
        Offers.delete(id)
        Ok(Json.obj(
          "status" -> "OK"
        ))
    }
  }

  /**
   *
   * @param offerId
   * @param pictureName
   * @return
   */
  def deletePicture(offerId: Long, pictureName: String) = DBAction { implicit rs =>
    val file = new File(configuration.getString("pictures_dir").get + pictureName + ".jpg")
    if (file.exists) file.delete
    Pictures.deletePictureByName(offerId, pictureName)
    Ok(Json.obj(
      "status" -> "OK"
    ))
  }

  /**
   *
   * @param offerid
   * @param tick
   * @return
   */
  def updateListing(offerid: Long, tick: String) = DBAction(parse.raw) { implicit  rs =>
    rs.request.body.asBytes(maxLength = 1024) match {
      case None =>
        BadRequest(Json.obj(
          "status" -> "KO",
          "message" -> "no post body"
        ))
      case Some(body) =>
        val pass = password + tick
        val decryptedBody = appCryptor.decryptData(body, pass.toCharArray)
        val json = Json.parse(decryptedBody)
        val title = (json \ "title").asOpt[String]
        val description = (json \ "description").asOpt[String]
        val telephone = (json \ "telephone").asOpt[String]
        val price = (json \ "price").asOpt[Double]
        val shop = (json \ "shop").asOpt[String]
        val pictureList = (json \ "pictures").asOpt[Seq[JsString]]
        Transactions.findPendingOrProcessingTransById(offerid) match {
          case Some(trans) =>
            Ok(Json.obj(
              "status" -> "KO",
              "message" -> "Can not update this listing while there are pending or processing transactions. Please attend to all associated transactions first."
            ))
          case None =>
            Offers.updateFields(offerid, title, description, price, shop, telephone)
            pictureList match {
              case Some(pictures) =>
                for {
                  picName <- pictures
                } yield Json.obj(
                  "name" -> picName.as[String],
                  "offerid" -> offerid
                ).validate[Picture].map { picture =>
                  Pictures.insert(picture)
                }
              case None =>
            }
            Ok(Json.obj(
              "status" -> "OK"
            ))
        }
    }
  }

  /**
   *
   * @param id
   * @param page
   * @return
   */
  def returnUsersRecords(id: Long, page: Int) = DBAction { implicit rs =>
    val buyingTransPage = Buyings.buyingTransByUserId(page = page, userid = id)
    val sellingTransPage = Sellings.sellingTransByUserId(page = page, userid = id)
    val messagesPage = Conversations.convsForUser(page = page, userid = id)
    Ok(Json.obj(
      "status" -> "OK",
      "messages_records" -> Json.toJson(messagesPage),
      "buying_records" -> Json.toJson(buyingTransPage),
      "selling_records" -> Json.toJson(sellingTransPage)
    ))
  }


  def getListing = DBAction { implicit rs =>
    val itemid = rs.queryString.get("id").get(0).toLong
    Offers.findListingById(itemid) match {
      case Some(listing) =>
        Locations.findZLocByOfferId(itemid) match {
          case Some(loc) =>
            Users.findById(listing.userid) match {
              case Some(user) =>
                Ok(html.listingItem(listing.title)(listing, listing.pictures.orNull, loc, user))
                /*
                Ok(Json.obj(
                  "status" -> "OK",
                  "listing" -> Json.obj(
                    "id" -> listing.id,
                    "title" -> listing.title,
                    "description" -> listing.description,
                    "price" -> listing.price,
                    "pictures" -> listing.pictures,
                    "shop" -> listing.shop,
                    "telephone" -> listing.telephone,
                    "userid" -> listing.userid,
                    "updated_on" -> listing.updated_on.orNull.toString),
                  "location" -> Json.toJson(loc),
                  "user" -> Json.toJson(user)
                ))
                */

              case None =>
                Ok(Json.obj(
                  "status" -> "KO"
                ))
            }
          case None =>
            Ok(Json.obj(
              "status" -> "KO"
            ))
        }
      case None =>
        Ok(Json.obj(
          "status" -> "KO"
        ))
    }
  }

  /**
   *
   * @param listingId
   * @return
   */
  def sendListingById(listingId: Long) = DBAction { implicit rs =>
    Offers.findListingById(listingId) match {
      case Some(listing) =>
        Locations.findZLocByOfferId(listingId) match {
          case Some(loc) =>
            Users.findById(listing.userid) match {
              case Some(user) =>
                Ok(Json.obj(
                  "status" -> "OK",
                  "listing" -> Json.obj(
                    "id" -> listing.id,
                    "title" -> listing.title,
                    "description" -> listing.description,
                    "price" -> listing.price,
                    "pictures" -> listing.pictures,
                    "shop" -> listing.shop,
                    "telephone" -> listing.telephone,
                    "userid" -> listing.userid,
                    "updated_on" -> listing.updated_on.orNull.toString),
                  "location" -> Json.toJson(loc),
                  "user" -> Json.toJson(user)
                ))
              case None =>
                Ok(Json.obj(
                  "status" -> "KO"
                ))
            }
          case None =>
            Ok(Json.obj(
              "status" -> "KO"
            ))
        }
      case None =>
        Ok(Json.obj(
          "status" -> "KO"
        ))
    }
  }
  /**
   *
   * @return
   */
  def receiveTransaction = DBAction(parse.json) { implicit rs =>
    rs.request.body.validate[Transaction].map { transaction =>
      Transactions.insertReturningId(transaction) match {
        case Some(transId) =>
          Json.obj(
            "status" -> "pending",
            "userid" -> transaction.buyerid,
            "transactionid" -> transId
          ).validate[Buying].map { buyTrans =>
            Buyings.insert(buyTrans)
            Json.obj(
              "status" -> "pending",
              "userid" -> transaction.sellerid,
              "transactionid" -> transId
            ).validate[Selling].map { sellTrans =>
              Sellings.insert(sellTrans)
              Ok(Json.obj(
                "status" -> "OK",
                "sell_trans" -> Json.toJson(sellTrans)
              ))
            }.getOrElse(InternalServerError(Json.obj(
              "status" -> "OK",
              "message" -> "Unkown error while inserting selling record")))
          }.getOrElse(InternalServerError(Json.obj(
            "status" -> "OK",
            "message" -> "Unkown error while inserting buying record")))
        case None =>
          InternalServerError(Json.obj(
            "status" -> "KO",
            "message" -> "Unkown error while inserting transaction record"))
      }
    }.getOrElse {
      BadRequest(Json.obj(
        "status" -> "KO",
        "message" -> "invalid transaction"))
    }
  }

  /**
   *
   * @param transid
   * @return
   */
  def cancelTransaction(transid: Long) = DBAction { implicit rs =>
    Transactions.findPendingOrProcessingTransById(transid) match {
      case Some(trans) =>
        Ok(Json.obj(
          "status" -> "KO",
          "message" -> "forbidden"
        ))
      case None =>
        Transactions.delete(transid)
        Ok(Json.obj(
          "status" -> "OK",
          "message" -> "transaction deleted"))
    }
  }

  def acceptTransaction(transid: Long) = DBAction { implicit rs =>
    Sellings.acceptSellingTrans(transid)
    Ok(Json.obj(
      "status" -> "OK",
      "message" -> "transaction accepted"))
  }

  def completeTransaction(transid: Long) = DBAction { implicit rs =>
    Transactions.completeTransaction(transid)
    Ok(Json.obj(
      "status" -> "OK",
      "message" -> "transaction completed"))
  }

  def backdownFromDeal(transid: Long) = DBAction { implicit rs =>
    Transactions.failTransaction(transid)
    Ok(Json.obj(
      "status" -> "OK",
      "message" -> "deal cancelled"))
  }

  /**
   *
   * @param id
   * @return
   */
  def sendUserById(id: Long) = DBAction { implicit rs =>
    Users.findById(id) match {
      case Some(user) =>
        Ok(Json.obj(
          "status" -> "OK",
          "user" ->  Json.toJson(user))
        )
      case None =>
        Ok(Json.obj(
          "status" -> "KO")
        )
    }
  }

  def startConversation = DBAction(parse.json(maxLength = 2048)) { implicit rs =>
    (rs.request.body \ "conversation").validate[Conversation].map { conversation =>
      (rs.request.body \ "message").asOpt[String] match {
        case Some(raw_message) =>
        Conversations.insertReturningId(conversation) match {
          case Some(convId) =>
            Json.obj(
              "message" -> raw_message,
              "received_status" -> "unread",
              "convid" -> convId,
              "senderid" -> conversation.user1id,
              "recipientid" -> conversation.user2id
            ).validate[Message].map { message =>
              Messages.insert(message)
              Ok(Json.obj(
                "status" -> "OK",
                "message" -> Json.toJson(message))
              )
            }.getOrElse {
              Conversations.delete(convId)
              BadRequest(Json.obj(
                "status" -> "KO",
                "message" -> "unknown error while validating the message"))
            }
          case None =>
            BadRequest(Json.obj(
              "status" -> "KO",
              "message" -> "inserting did not return an id"))
        }
        case None =>
          BadRequest(Json.obj(
            "status" -> "KO",
            "message" -> "there was not message in the request"))
      }
    }.getOrElse(BadRequest(Json.obj(
      "status" -> "KO",
      "message" -> "invalid conversation posted")))
  }

  def markConvoRead(convid: Long) = DBAction { implicit rs =>
    Messages.markAsReadByConvId(convid)
    Ok(Json.obj(
      "status" -> "OK"
    ))
  }

  def leaveConvo(convid: Long, userid: Long) = DBAction { implicit rs =>
    Conversations.userLeaveConvo(convid, userid)
    Ok(Json.obj(
      "status" -> "OK"
    ))
  }

  def replyToConvo = DBAction(parse.json(maxLength = 1024)) { implicit rs =>
    rs.request.body.validate[Message].map {
      message =>
        Messages.insert(message)
        Ok(Json.obj(
          "status" -> "OK",
          "message" -> Json.toJson(message)
        ))
    }.getOrElse(BadRequest(Json.obj(
      "status" -> "KO",
      "message" -> "bad message"
    )))
  }

  def getConversationsForUser(page: Int, userid: Long) = DBAction { implicit rs =>
    val conversationsPage = Conversations.convsForUser(page = page, userid = userid)
    Ok(Json.obj(
      "status" -> "OK",
      "conversations" -> Json.toJson(conversationsPage)
    ))
  }

  def submitFeedback = DBAction(parse.json(maxLength = 1024)) { implicit  rs =>
    (rs.request.body \ "feedback").validate[Feedback].map { feedback =>
      Feedbacks.findByUserWithTransid(feedback.transid, feedback.by_userid) match {
        case None =>
          (rs.request.body \ "rating").asOpt[Int] match {
            case Some(rating) =>
              val feedbackid = Feedbacks.insertReturningId(feedback)
              Json.obj(
                "rating" -> rating,
                "userid" -> feedback.userid,
                "by_userid" -> feedback.by_userid,
                "transid" -> feedback.transid,
                "feedbackid" -> feedbackid
              ).validate[Rating].map { newRating =>
                Ratings.insert(newRating)
                Ok(Json.obj(
                  "status" -> "OK",
                  "rating" -> Json.toJson(rating),
                  "feedback" -> Json.toJson(feedback)
                ))
              }.getOrElse(BadRequest(Json.obj(
                "status" -> "KO",
                "message" -> "bad rating"
              )))
            case None =>
              Feedbacks.insert(feedback)
              Ok(Json.obj(
                "status" -> "OK",
                "feedback" -> Json.toJson(feedback)
              ))
          }
        case _ =>
          BadRequest(Json.obj(
            "status" -> "KO",
            "message" -> "feedback already given for this transaction"
          ))
      }
    }.getOrElse(BadRequest(Json.obj(
      "status" -> "KO",
      "message" -> "bad feedback"
    )))
  }

  /**
   *
   * @param userid
   * @return
   */
  def returnRatingForUser(userid: Long) = DBAction { implicit rs =>
      Ok(Json.obj(
        "status" -> "OK",
        "conversations" -> Json.toJson(Ratings.ratingForUser(userid))
      ))
  }

  /**
   *
   * @return
   */
  def returnRatingForTransactions(by_userid: Long) = DBAction(parse.json(maxLength = 1024)) { implicit rs =>
    (rs.request.body \ "completedTransids").asOpt[List[Long]] match {
      case Some(Nil) =>
        (rs.request.body \ "failedTransids").asOpt[List[Long]] match {
          case Some(failedIds) =>
            val feedbacksList = Feedbacks.findFailedFeedback(failedIds, by_userid)
            Ok(Json.obj(
              "status" -> "OK",
              "failedFeedbacks" -> Json.toJson(feedbacksList),
              "completedFeedbacks" -> JsArray()
            ))
          case None =>
            BadRequest(Json.obj(
              "status" -> "KO",
              "message" -> "bad ratings request - no transids list found"
            ))
        }
      case Some(completedIds) =>
        (rs.request.body \ "failedTransids").asOpt[List[Long]] match {
          case Some(Nil) =>
            val completedFeedbacksList = Feedbacks.findCompletedFeedback(completedIds, by_userid)
            Ok(Json.obj(
              "status" -> "OK",
              "failedFeedbacks" -> JsArray(),
              "completedFeedbacks" -> Json.toJson(completedFeedbacksList)
            ))
          case Some(failedIds) =>
            val completedFeedbacksList = Feedbacks.findCompletedFeedback(completedIds, by_userid)
            val failedFeedbacksList = Feedbacks.findFailedFeedback(failedIds, by_userid)
            Ok(Json.obj(
              "status" -> "OK",
              "failedFeedbacks" -> Json.toJson(failedFeedbacksList),
              "completedFeedbacks" -> Json.toJson(completedFeedbacksList)
            ))
        }
    }
  }

  /**
   *
   * @param userid
   * @return
   */
  def returnFeedbacksForUser(userid: Long) = DBAction { implicit rs =>
    Feedbacks.findForUser(userid) match {
      case Some(feedbacks) =>
        val rating = Ratings.ratingForUser(userid)
        Ok(Json.obj(
          "feedbacks" -> Json.toJson(feedbacks),
          "rating" -> Json.toJson(rating)
        ))
      case None =>
        Ok(Json.obj(
          "feedbacks" -> JsArray(),
          "rating" -> JsNull
        ))
    }
  }

  /**
   *
   * @return
   */
  def returnBillingForUser = Action.async(parse.raw) { implicit request =>
    request.body.asBytes(maxLength = 1024) match {
      case Some(body) =>
        val userid = request.queryString.get("id").get(0)
        val sig = request.queryString.get("sig").get(0)
        val tick = request.queryString.get("tick").get(0)
        val sigBytes = Base64.decodeBase64(sig.getBytes)
        val decodedString = new String(sigBytes)
        val pass = password+tick
        val decryptedBytes = appCryptor.decryptData(body, pass.toCharArray)
        val decryptedString = new String(decryptedBytes)
        if (decodedString == decryptedString) {
          DB.withSession { implicit s =>
            val billing = Billings.billingsByUserId(userid.toLong)
            import implicitBillingWrites._
            billing.get("unpaid") match {
              case Some(Nil) =>
                Future(Ok(Json.obj(
                  "status" -> "OK",
                  "billing" -> Json.toJson(billing)
                )))
              case Some(unpaidBilling) =>
                val rateTo = "USD"
                val respSeq = (for {
                  ub <- unpaidBilling
                  url = "http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.xchange%20where%20pair%20in%20%28%22"+ub.currency_code+rateTo+"%22%29&format=json&env=store://datatables.org/alltableswithkeys&callback="
                } yield {
                  WS.url(url).get().map { resp =>
                    (ub, (resp.json \ "query" \ "results" \ "rate" \ "Rate").as[String])
                  }
                }).toSeq
                Future.sequence(respSeq).map { case listValue =>
                  Ok(Json.obj(
                    "status" -> "OK",
                    "billing" -> Json.obj(
                      "unpaid" -> JsArray(
                        for {
                          v <- listValue
                        } yield {
                          Json.obj(
                            "bill" -> Json.toJson(v._1),
                            "USD_Exchange" -> JsString(v._2)
                          )
                        }
                      ),
                      "paid" -> JsArray(
                        billing.get("paid") match {
                          case Some(paidBills) =>
                            for {
                              b <- paidBills
                            } yield {
                              Json.obj(
                                "bill" -> Json.toJson(b)
                              )
                            }
                          case None =>
                            Nil
                        }
                      )
                    )
                  ))
                }
              case None =>
                Future(Ok(Json.obj(
                  "status" -> "OK",
                  "billing" -> Json.toJson(billing)
                )))
            }
          }
        }
        else
          Future(Unauthorized(Json.obj(
            "status" -> "KO",
            "message" -> "not authorized"
          )))
      case None =>
        Future(BadRequest("no body found"))
    }
  }

  /**
   *
   * @return
   */
  def merchantData = DBAction(parse.raw) { implicit rs =>
    rs.request.body.asBytes(maxLength = 1024) match {
      case Some(body) =>
        val tick = rs.request.queryString.get("tick").get(0)
        val pass = password+tick
        val decryptedBytes = appCryptor.decryptData(body, pass.toCharArray)
        Json.parse(decryptedBytes).validate[Merchant].map { merchant =>
          Merchants.findByUserId(merchant.userid) match {
            case Some(existingMerchant) =>
              if (merchant.secret == "" || merchant.identifier == "")
                Users.updateMerchant(isMerchant = false, merchant.userid)
              else Users.updateMerchant(isMerchant = true, merchant.userid)
              Merchants.update(existingMerchant.id.get, merchant)
              Ok(Json.obj(
                "status" -> "OK",
                "merchant" -> Json.toJson(merchant),
                "existing id" -> JsNumber(existingMerchant.id.get)
              ))
            case None =>
              Merchants.insert(merchant)
              Users.updateMerchant(isMerchant = true, merchant.userid)
              Ok(Json.obj(
                "status" -> "OK",
                "merchant" -> Json.toJson(merchant)
              ))
          }
        }.getOrElse(BadRequest(Json.obj(
          "status" -> "KO",
          "message" -> "bad merchant"
        )))
      case None =>
        BadRequest(Json.obj(
          "status" -> "KO",
          "message" -> "bad request"
        ))
    }
  }

  /**
   *
   * @param userid
   * @return
   */
  def followingFriends(userid: Long) = DBAction(parse.json(maxLength = 1024)) { implicit rs =>
    val friends = for {
      fs <- rs.request.body.as[List[JsValue]]
    } yield {
      fs.validate[Friend].get
    }
    Friends.updateFollowingForUser(userid, friends)
    Ok(Json.obj(
      "status" -> "OK",
      "message" -> "following friends updated"
    ))
  }

  /**
   *
   * @return
   */
  def fbDeauthorized = Action {
    Ok(Json.obj(
       "status" -> "OK"
    ))
  }

}