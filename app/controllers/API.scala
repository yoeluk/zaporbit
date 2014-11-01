package controllers

import _root_.java.util

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
import _root_.java.lang.{Integer => jInt}
import _root_.java.util.{Arrays => JArrays}
import play.api.libs.functional.syntax._
import java.io.File

import models._

import org.cryptonode.jncryptor._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.db.slick.Config.driver._

import securesocial.core._
import service.SocialUser

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
    val body = views.html.welcomeEmail.render(contact).body
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

object writers {

  import FormMappings._

  case class RawFriend(userid: Long, friendfbid: String)
  case class RawMerchant(identifier: String, secret: String)
  case class RawMessage(toUser: Long, title: String, offerid: Long, message: String)

  /*
   * reference from accepted answer in stackoverlow
   * http://stackoverflow.com/questions/17281291/how-do-i-write-a-json-format-for-an-object-in-the-java-library-that-doesnt-have
   */
  implicit val rdsTimestamp: Reads[Timestamp] = (__ \ "time").read[Long].map{ long => new Timestamp(long) }
  implicit val wrsTimestamp: Writes[Timestamp] = (__ \ "time").write[String].contramap{ (a: Timestamp) => a.toString }
  implicit val fmtTimestamp: Format[Timestamp] = Format(rdsTimestamp, wrsTimestamp)

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
            "listingid" -> item._1.offerid.get,
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
      Json.obj(
        "listings" -> JsArray(
          for {
            item <- page.items
          } yield Json.obj(
            "listing" -> Json.obj(
              "id" -> item._1.id,
              "title" -> item._1.title,
              "description" -> item._1.description,
              "price" -> item._1.price,
              "locale" -> item._1.locale,
              "currency_code" -> item._1.currency_code,
              "formatted_price" -> Wallet.displayCurrency(item._1.locale, item._1.price),
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
        ),
        "paging" -> Json.obj(
          "page" -> page.page,
          "offset" -> page.offset,
          "total" -> page.total
        )
      )
    }
  }
  implicit val implicitListingZOLocationWithRatingsWrites = new Writes[(Page[(Listing, ZOLocation, User)], Map[Long, (Int, Int)])] {
    def writes(res: (Page[(Listing, ZOLocation, User)], Map[Long, (Int, Int)])): JsValue = {
      Json.obj(
        "listings" -> JsArray(
          for {
            item <- res._1.items
          } yield Json.obj(
            "listing" -> Json.obj(
              "id" -> item._1.id,
              "title" -> item._1.title,
              "description" -> item._1.description,
              "price" -> item._1.price,
              "locale" -> item._1.locale,
              "currency_code" -> item._1.currency_code,
              "formatted_price" -> Wallet.displayCurrency(item._1.locale, item._1.price),
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
              "rating" -> res._2.getOrElse(item._3.id.get, (5,1))._1,
              "ratingCount" -> res._2.getOrElse(item._3.id.get, (5,1))._2,
              "email" -> item._3.email,
              "created_on" -> item._3.created_on
            )
          )
        ),
        "paging" -> Json.obj(
          "page" -> res._1.page,
          "offset" -> res._1.offset,
          "total" -> res._1.total
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
            "currency_code" -> item._1.currency_code,
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
          "currency_code" -> item.currency_code,
          "userid" -> item.userid,
          "formatted_price" -> Wallet.displayCurrency(item.locale, item.price),
          "pictures" -> item.pictures.get,
          "highlight" -> item.highlight,
          "waggle" -> item.waggle,
          "shop" -> item.shop,
          "updated_on" -> item.updated_on.orNull.toString
        )
      )
    }
  }
  implicit val implicitOffersForUserWrites = new Writes[Page[(Offer, OfferStatus, OfferPicture)]] {
    def writes(page: Page[(Offer, OfferStatus, OfferPicture)]): JsValue = {
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
            "currency_code" -> item._1.currency_code,
            "userid" -> item._1.userid,
            "highlight" -> item._1.highlight,
            "waggle" -> item._1.waggle,
            "shop" -> item._1.shop,
            "updated_on" -> item._1.updated_on.orNull.toString
          ),
          "listingStatus" -> Json.obj(
            "status" -> item._2.status
          ),
          "listingPicture" -> Json.obj(
            "name" -> item._3.name
          )
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
    def writes(t: (Float,Int)): JsValue = {
      Json.obj(
        "rating" -> t._1.toString,
        "total_ratings" -> t._2
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
      "currency_code" -> nonEmptyText,
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
      "fbuserid" -> nonEmptyText,
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
        "wallet_order_id" -> bill.googlewallet_id.orNull.toString,
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
      "friendid" -> longNumber,
      "friendfbid" -> nonEmptyText
    )(Friend.apply)(Friend.unapply)
  )

  /**** RawFriend validation ****/

  implicit val rawFriendFormat = Json.format[RawFriend]
  val rawFriendForm = Form(
    mapping(
      "userid" -> longNumber,
      "friendfbid" -> nonEmptyText
    )(RawFriend.apply)(RawFriend.unapply)
  )

  /**** RawMerchant validation ****/

  implicit val rawMerchantFormat = Json.format[RawMerchant]
  val rawMerchantForm = Form(
    mapping(
      "identifier" -> nonEmptyText,
      "secret" -> nonEmptyText
    )(RawMerchant.apply)(RawMerchant.unapply)
  )

  /**** RawMessage validation ****/

  implicit val rawMessageFormat = Json.format[RawMessage]
  val rawMessageForm = Form(
    mapping(
      "toUser" -> longNumber,
      "title" -> nonEmptyText,
      "offerid" -> longNumber,
      "message" -> nonEmptyText
    )(RawMessage.apply)(RawMessage.unapply)
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
}

class API(override implicit val env: RuntimeEnvironment[SocialUser]) extends securesocial.core.SecureSocial[SocialUser] {
  import AppCryptor._

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def pictureUrl(userid: Long, fbuserid: String)(implicit s: simple.Session): String = {
    UserOptions.findByUserid(userid.toLong) match {
      case None =>
        "//graph.facebook.com/v2.1/" + fbuserid + "/picture?height=200&width=200"
      case Some(opts) =>
        val parts = opts.picture.get.split("\\.")
        if (parts.length > 1) "/options/pictures/300/" + opts.picture.get
        else "/options/pictures/300/" + opts.picture.get + ".jpg"
    }
  }

  import writers._
  /**************************/
  /**** REQUESTS API ****/
  /**************************/

  def listingsByLoc(page: Int, radius: Int) = UserAwareAction(parse.json) { implicit request =>
    DB.withSession { implicit s =>
      request.user match {
        case Some(user) =>
          (request.body \ "location").validate[ZOLocation].map { loc =>
            val pageResult = Locations.listByLoc(page = page, radius = radius, loc = loc, userid = user.main.id.get)
            //val pageResult = Locations.testListByLoc(page = page, radius = radius, loc = loc, userid = userid)
            Ok(Json.toJson(pageResult))
          }.getOrElse(BadRequest(Json.obj(
            "status" -> "KO",
            "message" -> "invalid location"))
            )
        case None =>
          (request.body \ "location").validate[ZOLocation].map { loc =>
            val userid = request.queryString.get("id").flatMap(_.headOption).getOrElse("0").toLong
            val pageResult = Locations.listByLoc(page = page, radius = radius, loc = loc, userid = userid)
            //val pageResult = Locations.testListByLoc(page = page, radius = radius, loc = loc, userid = userid)
            Ok(Json.toJson(pageResult))
          }.getOrElse(BadRequest(Json.obj(
            "status" -> "KO",
            "message" -> "invalid location"))
            )
      }
    }
  }


  def filterLocation(page: Int) = UserAwareAction(parse.json) { implicit request =>
    DB.withSession { implicit s =>
      request.user match {
        case Some(user) =>
          (request.body \ "location").validate[ZOLocation].map { loc =>
            val filter = request.queryString.get("filter").get(0)
            val pageResult = Locations.filterLoc(page = page, loc = loc, filterStr = filter, userid = user.main.id.get)
            //val pageResult = Locations.testListByLoc(page = page, radius = radius, loc = loc, userid = userid)
            Ok(Json.toJson(pageResult))
          }.getOrElse(BadRequest(Json.obj(
            "status" -> "KO",
            "message" -> "invalid location"))
            )
        case None =>
          (request.body \ "location").validate[ZOLocation].map { loc =>
            val filter = request.queryString.get("filter").get(0)
            val userid = request.queryString.get("id").flatMap(_.headOption).getOrElse("0").toLong
            val pageResult = Locations.filterLoc(page = page, loc = loc, filterStr = filter, userid = userid)
            //val pageResult = Locations.testListByLoc(page = page, radius = radius, loc = loc, userid = userid)
            Ok(Json.toJson(pageResult))
          }.getOrElse(BadRequest(Json.obj(
            "status" -> "KO",
            "message" -> "invalid location"))
            )
      }
    }
  }

  /**
   *
   * @param page the page
   * @param orderBy order by
   * @return
   */
  def listingsByUser(page: Int, orderBy: Int) = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit rs =>
          val pageResult = Offers.list1(page = page, orderBy = orderBy, userId = user.id.get)
          Ok(Json.toJson(pageResult))
        }
    }
  }

  def listingsForUser(userid: Long, page: Int, orderBy: Int) = DBAction { implicit rs =>
    DB.withSession { implicit rs =>
      val pageResult = Offers.list2(page = page, orderBy = orderBy, userId = userid)
      Ok(Json.toJson(pageResult))
    }
  }

  /**
   *
   * @param page
   * @param orderBy
   * @return
   */
  def offersByUser(page: Int, orderBy: Int) = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit rs =>
          val pageResult = Offers.offersForUser(page = page, orderBy = orderBy, userId = user.id.get)
          Ok(Json.toJson(pageResult))
        }
    }
  }

  def updateListingStatus = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit rs =>
          (request.body \ "listingid").asOpt[Long] match {
            case Some(listingid) =>
              (request.body \ "status").asOpt[String] match {
                case Some(status) =>
                  ListingStatuses.update(listingid, status)
                  Ok(Json.obj(
                    "message" -> "listing published",
                    "status" -> "OK"
                  ))
              }
          }
        }
    }
  }

  def updateUserOptions = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit rs =>
          val about = (request.body \ "about").asOpt[String]
          val background = (request.body \ "backgroundPicture").asOpt[String]
          val picture = (request.body \ "profilePicture").asOpt[String]
          val options = UserOption(userid = user.id.get, background = background, picture = picture, about = about)
          UserOptions.insertOrUpdate(options)
          Ok(Json.obj(
            "status" -> "OK",
            "message" -> "the user's options were updated"
          ))
        }
    }
  }

  def saveOptionsToDisk(name: String, optionType: String) = Action.async(parse.file(to = new File(configuration.getString("options_dir").get + name)) ) {
    implicit request =>
      // this is ugly... oh well
      val s: jInt = 300
      val l: jInt = 1700
      if (optionType == "bkg") ServeScaledImage.saveScaledImage(JArrays.asList(l), request.body)
      else if (optionType == "pic") ServeScaledImage.saveScaledImage(JArrays.asList(s), request.body)
      Future.successful {
        Ok(Json.obj(
          "status" -> "OK",
          "message" -> JsString("picture " + name + " was uploaded.")
        ))
      }
  }

  /*
  case class UpgradeListing(offerid: Long, waggle: Boolean, highlight: Boolean)

  implicit val upgradeFormat = Json.format[UpgradeListing]
  val upgradeForm = Form(
    mapping(
      "offerid" -> longNumber,
      "waggle" -> boolean,
      "highlight" -> boolean
    )(UpgradeListing.apply)(UpgradeListing.unapply)
  )

  def upgradeListing(tick: String) = DBAction(parse.raw) { implicit rs =>
    rs.request.body.asBytes(maxLength = 1024) match {
      case Some(body) =>
        val pass = password + tick
        val decryptedBody = appCryptor.decryptData(body, pass.toCharArray)
        Json.parse(decryptedBody).validate[UpgradeListing].map { upgrade =>
          Redirect(routes.Application.index())
        }.getOrElse(BadRequest(Json.obj(
          "status" -> "KO",
          "message" -> ""
        )))
      case None =>
        BadRequest("no post body found")
    }
  }
  */

  /**
   *
   * @param name
   * @return
   */
  def downloadPicture(name: String) = Action {
    val parts = name.split("\\.", -1)
    val ext = if (parts.length > 1) "" else ".jpg"
    Ok.sendFile(new File(configuration.getString("pictures_dir").get + name + ext))
  }

  /**
   *
   * @param name
   * @return
   */
  def savePictureToDisk(name: String) = Action.async(parse.file(to = new File(configuration.getString("pictures_dir").get + name ))) {
    implicit request =>
      // I will change this later to add these two task in parallel. Just need to return a Future from the other method :)
      val s: jInt = 600
      val l: jInt = 2000
      val file = new File(configuration.getString("pictures_dir").get + name)
      ServeScaledImage.saveScaledImage(JArrays.asList(s, l), file)
      Future.successful{
        Ok(Json.obj(
          "status" -> "OK",
          "message" -> JsString("picture " + name + " was uploaded.")
        ))
      }
  }
  /**
   * inserts a new listing item (offer + pictures)
   * @return
   */
  def receiveListing = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          (request.body \ "location").validate[Location] match {
            case JsSuccess(location, _) =>
              (request.body \ "offer").validate[Offer] match {
                case JsSuccess(pseudOffer, _) =>
                  val newOffer = pseudOffer.copy(userid = user.id.get)
                  val insertedOfferId = Offers.insertReturningId(newOffer)
                  for {
                    (name, i) <- (request.body \ "pictures").as[Seq[JsString]].zipWithIndex if i < 6
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
                  ))
                case JsError(e) =>
                  //println(JsError.toFlatJson(e))
                  BadRequest(Json.obj(
                    "status" -> "KO",
                    "message" -> "invalid listing json"))
              }
            case JsError(e) =>
              //println(JsError.toFlatJson(e))
              BadRequest(Json.obj(
                "status" -> "KO",
                "message" -> "invalid location json"))
          }
        }
    }
  }
  /**
   * this method is called every time a user login (it inserts user if doesn't exists else updates user)
   * @return
   */
  def currentUser = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
//        if (oldUser.id.get == 21437) {
//          EmailService.sendWelcomeEmail(oldUser)
//        }
        DB.withSession { implicit s =>
          val rating = Ratings.ratingForUser(user.id.get)
          Ok(Json.obj(
            "status" -> "OK",
            "message" -> JsString("user " + user.name +
              " with old email " + user.email +
              " has been updated."),
            "userid" -> JsString("" + user.id.get),
            "rating" -> rating
          ))
        }
    }
  }

  /**
   *
   * @param listingid the id of the listing to delete
   * @return
   */
  def deleteListing(listingid: Long) = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          Transactions.findPendingOrProcessingTransById(listingid) match {
            case Some(trans) =>
              Forbidden(Json.obj(
                "status" -> "KO",
                "message" -> "Forbidden to modify a pending or processing transaction."
              ))
            case None =>
              val pictures = Pictures.picturesByOfferId(listingid)
              pictures.foreach { p =>
                val parts = p.split("\\.")
                val pName = if (parts.length > 1) p else p + ".jpg"
                val file = new File(configuration.getString("pictures_dir").get + pName)
                if (file.exists) file.delete
              }
              Offers.delete(listingid)
              Ok(Json.obj(
                "status" -> "OK"
              ))
          }
        }
    }
  }

  /**
   *
   * @param offerId the id of the offer
   * @param pictureName the picture name to delete
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
   * @param page
   * @return
   */
  def returnUsersRecords(page: Int) = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          val id = user.id.get
          val buyingTransPage = Buyings.buyingTransByUserId(page = page, userid = id)
          val sellingTransPage = Sellings.sellingTransByUserId(page = page, userid = id)
          val messagesPage = Conversations.convsForUser(page = page, userid = id)
          val billingPage = Billings.billingsByUserId(userid = id)
          Ok(Json.obj(
            "status" -> "OK",
            "messages_records" -> Json.toJson(messagesPage),
            "buying_records" -> Json.toJson(buyingTransPage),
            "selling_records" -> Json.toJson(sellingTransPage),
            "billing_records" -> Json.toJson(billingPage)
          ))
        }
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
  def cancelTransaction(transid: Long) = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          Transactions.findPendingOrProcessingTransById(transid) match {
            case Some(trans) =>
              Ok(Json.obj(
                "status" -> "KO",
                "message" -> "forbidden"
              ))
            case None =>
              Buyings.findByTransid(transid) match {
                case Some(t) =>
                  if (t.userid == user.id.get) {
                    Buyings.delete(t.id.get)
                    Ok(Json.obj(
                      "status" -> "OK",
                      "message" -> "transaction deleted"))
                  } else Unauthorized("")
                case None =>
                  Sellings.findByTransid(transid) match {
                    case Some(t) =>
                      if (t.userid == user.id.get) {
                        Sellings.delete(t.id.get)
                        Ok(Json.obj(
                          "status" -> "OK",
                          "message" -> "transaction deleted"))
                      } else Unauthorized("")
                    case None =>
                      BadRequest("")
                  }
              }
          }
        }
    }
  }

  def acceptTransaction(transid: Long) = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          Transactions.findById(transid) match {
            case Some(t) =>
              if (t.sellerid == user.id.get) {
                Sellings.acceptSellingTrans(transid)
                ListingStatuses.update(offerid = t.offerid, status = "committed")
                Ok(Json.obj(
                  "status" -> "OK",
                  "message" -> "transaction accepted"))
              }
              else
                Unauthorized("")
          }
        }
    }
  }

  def completeTransaction(transid: Long) = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          if (Transactions.completeTransaction(transid = transid, userid = user.id.get)) {
            Ok(Json.obj(
              "status" -> "OK",
              "message" -> "transaction completed"))
          }
          else
            Unauthorized("insufficient privileges")
        }
    }
  }

  def backdownFromDeal(transid: Long) = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          if (Transactions.failTransaction(transid = transid, userid = user.id.get)) {
            Ok(Json.obj(
              "status" -> "OK",
              "message" -> "deal cancelled"))
          }
          else
            Unauthorized("insufficient privileges")
        }
    }
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

  def startConversation = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        request.body.asOpt[RawMessage] match {
          case Some(rawMessage) =>
            if (user.id.get != rawMessage.toUser) {
              val convo = Conversation(
                user1_status = "online",
                user2_status = "online",
                user1id = user.id.get,
                user2id = rawMessage.toUser,
                title = rawMessage.title,
                offerid = Some(rawMessage.offerid))
              DB.withSession { implicit s =>
                Conversations.insertReturningId(convo) match {
                  case Some(convId) =>
                    val msg = Message(
                      message = rawMessage.message,
                      received_status = "unread",
                      convid = convId,
                      senderid = convo.user1id,
                      recipientid = convo.user2id)
                    Messages.insert(msg)
                    Ok(Json.obj(
                      "status" -> "OK",
                      "message" -> "the messages was sent successfully"
                    ))
                  case None =>
                    InternalServerError("An error ocurrered while inserting a new conversation. No ID was returned where one was expected.")
                }
              }
            } else BadRequest(Json.obj(
              "status" -> "KO",
              "message" -> "You can't send messages to yourself."
            ))
          case None =>
            BadRequest("invalid message")
        }
    }
  }

  def markConvoRead(convid: Long) = DBAction { implicit rs =>
    Messages.markAsReadByConvId(convid)
    Ok(Json.obj(
      "status" -> "OK"
    ))
  }

  def leaveConvo(convid: Long) = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          Conversations.userLeaveConvo(convid, user.id.get)
          Ok(Json.obj(
            "status" -> "OK"
          ))
        }
    }
  }

  def replyToConvo = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        Json.obj(
          "senderid" -> user.id.get,
          "received_status" -> "unread",
          "convid" -> (request.body \ "convid").as[Long],
          "message" -> (request.body \ "message").as[String],
          "recipientid" -> (request.body \ "recipientid").as[Long]
        ).validate[Message].map { message =>
          DB.withSession { implicit s =>
            Messages.insertReturningId(message) match {
              case None =>
                Ok(Json.obj(
                  "status" -> "OK",
                  "message" -> Json.toJson(message)
                ))
              case Some(id) =>
                Ok(Json.obj(
                  "status" -> "OK",
                  "withId" -> id,
                  "message" -> Json.toJson(message)
                ))
            }
          }
        }.getOrElse(BadRequest(Json.obj(
          "status" -> "KO",
          "message" -> "bad message"
        )))
    }
  }

  /**
   *
   * @param page
   * @param userid
   * @return
   */
  def getConversationsForUser(page: Int, userid: Long) = DBAction { implicit rs =>
    val conversationsPage = Conversations.convsForUser(page = page, userid = userid)
    Ok(Json.obj(
      "status" -> "OK",
      "conversations" -> Json.toJson(conversationsPage)
    ))
  }

  /**
   *
   * @return
   */
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
   * @param userid user id
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
  def merchantData = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          request.body.asOpt[RawMerchant] match {
            case Some(rawMerchant) =>
              val merchant = Merchant(userid = user.id.get, identifier = rawMerchant.identifier, secret = rawMerchant.secret)
              Merchants.findByUserId(user.id.get) match {
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
            case None =>
              BadRequest("invalid data in json")
          }
        }
    }
  }

  /**
   *
   * @return
   */
  def merchantInfo = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          Merchants.findByUserId(user.id.get) match {
            case Some(merchant) =>
              Ok(Json.obj(
                "merchant" -> Json.toJson(merchant),
                "userid" -> user.id.get
              ))
            case None =>
              Ok(Json.obj(
                "userid" -> user.id.get
              ))
          }
        }
    }
  }

  /**
   *
   * @return
   */
  def followingFriends = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          val friends = Friends.findFollowingForUser(user.id.get)
          val followings = for {
            f <- friends
          } yield {
            Users.findByFbId(f.friendfbid) match {
              case Some(dbUser) =>
                Json.obj(
                  "userid" -> f.friendid,
                  "fbuserid" -> f.friendfbid,
                  "pictureUrl" -> pictureUrl(f.friendid, f.friendfbid),
                  "name" -> dbUser.name,
                  "surname" -> dbUser.surname,
                  "rating" -> Ratings.ratingsForUserId(f.userid)
                )
              case None =>
                Json.obj()
            }
          }
          Ok(Json.obj(
            "status" -> "OK",
            "followings" -> Json.toJson(followings)
          ))
        }
    }
  }

  /**
   *
   * @return
   */
  def updateFollowingFriends = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          val friends = for {
            fs <- request.body.as[List[RawFriend]]
          } yield {
            val friendid = Users.findByFbId(fs.friendfbid) match {
              case Some(friend) => friend.id.get
              case None => 0.toLong
            }
            if (friendid != 0) {
              Some(Friend(userid = user.id.get, friendid = friendid, friendfbid = fs.friendfbid))
            } else None
          }
          //println(friends)
          Friends.updateFollowingForUser(user.id.get, friends)
          //println(Friends.findFollowingForUser(user.id.get)) // * now tested that this method correctly updates the followingFirends * //
          Ok(Json.obj(
            "status" -> "OK",
            "message" -> "following friends updated"
          ))
        }
    }
  }

  /**
   *
   * @return
   */
  def followThisUser = SecuredAction(parse.json) { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          (request.body \ "fbuserid").asOpt[String] match {
            case Some(friendfbid) =>
              (request.body \ "userid").asOpt[Long] match {
                case Some(friendid) =>
                  if (user.id.get != friendid && !Friends.isFollowing(userid = user.id.get, friendid = friendid)) {
                    Friends.insert( Friend(userid = user.id.get, friendid = friendid, friendfbid = friendfbid) )
                  }
                  Ok(Json.obj(
                    "status" -> "OK",
                    "message" -> "following added"
                  ))
                case None =>
                  BadRequest(Json.obj(
                    "status" -> "KO",
                    "message" -> "friendid not found"
                  ))
              }
            case None =>
              BadRequest(Json.obj(
                "status" -> "KO",
                "message" -> "friendfbid not found"
              ))
          }
        }
    }
  }

  /**
   *
   * @param friendid
   * @return
   */
  def unfollowFriend(friendid: Long) = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          Friends.deleteFollowing(userid = user.id.get, friendid = friendid)
          Ok(Json.obj(
            "status" -> "OK",
            "message" -> ""
          ))
        }
    }
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