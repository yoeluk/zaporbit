package controllers

import java.text.NumberFormat
import java.util.Locale

import play.api._
import play.api.mvc._
import play.api.libs.json._

import com.google.wallettools._
import models._
import play.api.db.slick._

/**
 * Created by yoelusa on 17/03/2014.
 */

object Wallet extends Controller {

  val seller = new Seller_Info
  val ISSUER = seller.ISSUER
  val SIGNING_KEY = seller.SIGNING_KEY

  import API.transactionFormat
  import API.buyingFormat
  import API.sellingFormat
  import models.Transactions.billingFormat

  def displayCurrency(localeIdentifier: String, price: Double): String = {
    val localeInfo = localeIdentifier.split("_")
    val aLocale = new Locale(localeInfo(0), localeInfo(1))
    val currencyFormatter = NumberFormat.getCurrencyInstance(aLocale)
    currencyFormatter.format(price)
  }

  def walletResponse = DBAction(parse.urlFormEncoded) { implicit rs =>
    rs.request.body.get("jwt") match {
      case Some(jwt: Seq[String]) =>
        val jwt_response = new JWT_Handler(ISSUER, SIGNING_KEY).deserialize(jwt(0))
        val json_response = Json.parse(jwt_response)
        val dataString = (json_response \ "request" \ "sellerData").as[String]
        val myData = parseMyData(dataString)
        myData.get("offerid") match {
          case Some(offerid) =>
            val waggle = myData.get("waggle").get.toBoolean
            val highlight = myData.get("highlight").get.toBoolean
            val price = (json_response \ "request" \ "price").as[String].toDouble
            val wallet_id = (json_response \ "response" \ "orderId").as[String]
            Offers.upgradeListing(offerid.toLong,waggle,highlight)
            Billings.insertPaidBillWithOfferid(offerid.toLong, price, wallet_id)
            Ok(wallet_id)
          case None =>
            myData.get("data") match {
              case Some(data) =>
                val ids = data.split("~").toList.map(_.toLong)
                val wallet_id = (json_response \ "response" \ "orderId").as[String]
                Billings.updatePaidBills(ids,wallet_id)
                Ok(wallet_id)
              case None =>
                BadRequest("")
            }
        }
      case None =>
        BadRequest("Unknown jwt parameter type")
    }
  }

  def merchantResponse(sellerid: Long) = DBAction(parse.urlFormEncoded) { implicit rs =>
    Merchants.findByUserId(sellerid) match {
      case Some(merchant) =>
        val ISSUER_ID = merchant.identifier
        val SIGNING_SECRETE = merchant.secret
        rs.request.body.get("jwt") match {
          case Some(jwt: Seq[String]) =>
            val jwt_response = new JWT_Handler(ISSUER_ID, SIGNING_SECRETE).deserialize(jwt(0))
            val json_response = Json.parse(jwt_response)
            val dataString = (json_response \ "request" \ "sellerData").as[String]
            val wallet_id = (json_response \ "response" \ "orderId").as[String]
            val myData = parseMyData(dataString)
            myData.get("offerid") match {
              case Some(offerid) =>
                myData.get("buyerid") match {
                  case Some(buyerid) =>
                    Offers.findById(offerid.toLong) match {
                      case Some(offer) =>
                        if (offer.userid == sellerid) {
                          Json.obj(
                            "status" -> "completed",
                            "offer_title" -> offer.title,
                            "offer_description" -> offer.description,
                            "offer_price" -> offer.price,
                            "currency_code" -> offer.currency_code,
                            "locale" -> offer.locale,
                            "buyerid" -> buyerid.toLong,
                            "sellerid" -> sellerid,
                            "offerid" -> offerid.toLong
                          ).validate[Transaction].map { transaction =>
                            Transactions.insertReturningId(transaction) match {
                              case Some(transId: Long) =>
                                Json.obj(
                                  "status" -> "completed",
                                  "userid" -> transaction.buyerid,
                                  "transactionid" -> transId
                                ).validate[Buying].map { buyTrans =>
                                  Buyings.insert(buyTrans)
                                  Json.obj(
                                    "status" -> "completed",
                                    "userid" -> transaction.sellerid,
                                    "transactionid" -> transId
                                  ).validate[Selling].map { sellTrans =>
                                    Sellings.insert(sellTrans)
                                    Json.obj(
                                      "status" -> "unpaid",
                                      "userid" -> sellerid,
                                      "offer_title" -> offer.title,
                                      "offer_description" -> offer.description,
                                      "offer_price" -> offer.price,
                                      "currency_code" -> offer.currency_code,
                                      "locale" -> offer.locale,
                                      "transactionid" -> transId).validate[Billing].map { bill =>
                                        Billings.insert(bill)
                                      Ok(wallet_id)
                                    }.getOrElse(BadRequest("invalid billing"))
                                  }.getOrElse(BadRequest("invalid  selling trans"))
                                }.getOrElse(BadRequest("invalid  buying trans"))
                              case None =>
                                BadRequest("internal error inserting transaction")
                            }
                          }.getOrElse(BadRequest("invalid transaction"))
                        } else
                          BadRequest("invalid seller id")
                      case None =>
                        BadRequest("invalid offer id")
                    }
                  case None =>
                    BadRequest("invalid buyer id")
                }
              case None =>
                BadRequest("offer id not found")
            }
          case None =>
            BadRequest("jwt not found")
        }
      case None =>
        BadRequest("merchant not found")
    }
  }

  def parseMyData(unparsedData:String): Map[String,String] = {
    val keyPairs = unparsedData.split(",")
    (for {
      kp <- keyPairs
    } yield {
      val kv = kp.split(":")
      (kv(0),kv(1))
    }).toMap
  }

  def index = Action {
    Ok("")
  }

  def generateToken(offer: Offer, identifier: String, secret: String, buyerid: Long): String = {
    val handler = new JWT_Handler(identifier, secret)
    handler.getJwt(offer,offer.id.get,buyerid)
  }

  def generateToken(offer: Offer, waggle: Boolean, highlight: Boolean): String = {
    val handler = new JWT_Handler(ISSUER, SIGNING_KEY)
    handler.getJwt(offer, offer.id.get, waggle, highlight)
  }

  def generateToken(userData: String, fees: String): String = {
    val handler = new JWT_Handler(ISSUER, SIGNING_KEY)
    handler.getJwt(userData, fees)
  }

}
