package controllers

//import _root_.java.util.concurrent

import _root_.java.text.NumberFormat
import _root_.java.util.concurrent.TimeoutException
import _root_.java.util.{Currency, Locale}
import controllers.Application._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.db.slick._

import securesocial.core._

import play.api.Play.current

import Wallet._
import models._
import views._
import service.SocialUser

import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import scala.math.BigDecimal

class Cart(override implicit val env: RuntimeEnvironment[SocialUser]) extends Controller with SecureSocial[SocialUser] {

  def displayCurrency(localeIdentifier: String, price: Double): String = {
    val localeInfo = localeIdentifier.split("_")
    val aLocale = new Locale(localeInfo(0), localeInfo(1))
    val currencyFormatter = NumberFormat.getCurrencyInstance(aLocale)
    currencyFormatter.format(price)
  }

  def upgradeListing(offerid: Long, waggle: Boolean, highlight: Boolean) = SecuredAction { implicit request =>
    DB.withSession { implicit s =>
      Pictures.firstPicturesFromOffer(offerid) match {
        case Some(thumbnail) =>
          Offers.findById(offerid) match {
            case Some(offer) =>
              if (offer.waggle != waggle || offer.highlight != highlight) {
                val token = generateToken(offer, waggle, highlight)
                Ok(views.html.upgrade(token, offer, thumbnail, waggle, highlight))
              } else BadGateway(Json.obj(
                "status" -> "KO",
                "message" -> "requested upgrade values match current values"
              ))
            case None =>
              BadGateway(Json.obj(
                "status" -> "KO",
                "message" -> "could not find a matching listing"
              ))
          }
        case None =>
          BadGateway(Json.obj(
            "status" -> "KO",
            "message" -> "could not find the thumbnail for the listing"
          ))
      }
    }
  }

  def purchaseItemFromMerchant(offerid: Long) = SecuredAction { implicit request =>
    DB.withSession { implicit s =>
      request.user.main match {
        case user: ExportedUser =>
          val thumbnail = Pictures.firstPicturesFromOffer(offerid).get
          Offers.findById(offerid) match {
            case Some(offer) =>
              Merchants.findByUserId(offer.userid) match {
                case Some(merchant) =>
                  val token = generateToken(offer,merchant.identifier, merchant.secret, user.id.get)
                  Ok(views.html.buy(token, offer, thumbnail))
                case None =>
                  BadRequest("")
              }

            case None =>
              BadGateway(Json.obj(
                "status" -> "KO",
                "message" -> "could not find a matching listing"
              ))
          }
        case _ =>
          BadRequest("")
      }
    }
  }

  def billingPayOut(userid: Long) = SecuredAction.async { implicit request =>
    request.user.main match {
      case user: ExportedUser =>
        if (user.id.get == userid) {
          DB.withSession { implicit s =>
            Billings.unpaidBillsForUser(userid) match {
              case bills: Map[_,_] =>
                bills.isEmpty match {
                  case true =>
                    Future(BadRequest(""))
                  case false =>
                    val codeTotal = for {
                      (k, bg) <- bills
                    } yield {
                      (k, (for {
                        b <- bg
                      } yield b.offer_price.toFloat).toList.sum)
                    }
                    val billsList = bills.values.toList.flatten
                    val rateTo = "USD"
                    val respSeq = (for {
                      (kCurrency, totalValue) <- codeTotal
                      url = "http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.xchange%20where%20pair%20in%20%28%22" + kCurrency + rateTo + "%22%29&format=json&env=store://datatables.org/alltableswithkeys&callback="
                    } yield {
                      WS.url(url).get().map { resp =>
                        (kCurrency, totalValue, (resp.json \ "query" \ "results" \ "rate" \ "Rate").as[String].toFloat)
                      }
                    }).toSeq
                    Future.sequence(respSeq).map { billsWithRates =>
                      val billsWithFees = (for {
                        b <- billsList
                      } yield {
                        for {
                          r <- billsWithRates
                          if b.currency_code == r._1
                        } yield {
                          (b, BigDecimal(b.offer_price.toFloat * r._3 * 0.06).setScale(2, BigDecimal.RoundingMode.HALF_UP))
                        }
                      }).flatten
                      val userData = (for((b,f)<- billsWithFees)yield b.id.get).mkString("~")
                      val fees = (for((b,f)<- billsWithFees)yield f).sum.toString()
                      val token = generateToken("data:"+userData, fees)
                      Ok(views.html.pay(token, billsWithFees))
                    }.recover {
                      case t: TimeoutException =>
                        RequestTimeout(t.getMessage)
                      case e: Throwable =>
                        ServiceUnavailable(e.getMessage)
                    }
                }
            }
          }
        } else Future(Redirect(routes.Cart.billingPayOut(user.id.get)))
      case _ =>
        Future(BadRequest(""))
    }
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(Routes.javascriptRouter("jsRoutes")(securesocial.controllers.routes.javascript.LoginPage.login)).as(JAVASCRIPT)
  }

}