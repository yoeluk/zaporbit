package controllers

import com.paypal.sdk.util.OAuthSignature
import java.util.Map
import play.api.libs.ws._
import play.api.mvc._
import play.api.libs.json._
import play.api.db.slick._
import play.api.Play.current
import play.api.cache.Cache

import securesocial.core._
import service.SocialUser

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration._
import play.api.libs.concurrent.Promise.timeout

import models._

class PayPal(override implicit val env: RuntimeEnvironment[SocialUser]) extends securesocial.core.SecureSocial[SocialUser] {

  def paySandbox = UserAwareAction.async { implicit request =>
    request.getQueryString("offerid") match {
      case Some(offerid) =>
        request.user match {
          case Some(socialUser) =>
            val user = socialUser.main
            DB.withSession { implicit s =>
              Offers.findWithUserById(offerid.toLong) match {
                case Some(goods) =>
                  PaypalMerchants.findByUserid(goods.provider.id.get) match {
                    case Some(merchant) =>
                      val url = "https://svcs.sandbox.paypal.com/AdaptivePayments/Pay"
                      val redirectUrl = "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_ap-payment&paykey="
                      val payload = Json.obj(
                        "requestEnvelope" -> Json.obj(
                          "errorLanguage" ->"en_US"),
                        "actionType" -> "PAY",
                        "returnUrl" -> "http://example.com/returnURL.htm",
                        "cancelUrl" -> "http://example.com/cancelURL.htm",
                        "currencyCode" -> "CAD",
                        "receiverList" -> Json.obj(
                          "receiver" -> Json.arr(Json.obj(
                            "email" -> "yoeluk@gmail.com", //goods.provider.email,
                            "amount" -> "10.00"))))
                      val method = OAuthSignature.HTTPMethod.POST
                      val payResponse = paypalRequestHolder(url).withHeaders(
                          "X-PAYPAL-DEVICE-IPADDRESS" -> request.remoteAddress,
                          "X-PAYPAL-AUTHORIZATION" -> paypalOAuthHeader(url = url, merchant = merchant, method = method)
                        ).post(payload)
                      val timeoutFuture = timeout("Oops", 15.seconds)
                      Future.firstCompletedOf(Seq(payResponse, timeoutFuture)).map {
                        case wsResponse: WSResponse =>
                          (wsResponse.json \ "responseEnvelope").asOpt[JsValue] match {
                            case Some(respEnv) =>
                              if ((respEnv \ "ack").as[String] == "Success") {
                                val payKey = (wsResponse.json \ "payKey").as[String]
                                Ok(Json.obj(
                                  "url" -> s"$redirectUrl$payKey",
                                  "status" -> "OK"))
                              } else BadRequest("The request returned a failure status")
                            case None =>
                              ServiceUnavailable("PayPal services are unavailable")
                          }
                        case error: String => InternalServerError(error)
                      }
                    case None =>
                      Future.successful(Ok("no merchant found"))
                  }
                case None =>
                  Future.successful(Ok("no goods found"))
              }
            }
          case None =>
            Future.successful(Ok("no user found"))
        }
      case None =>
        Future.successful(Ok("no offerid param found"))
    }
  }

  def paypalPermRequest = SecuredAction.async { implicit request =>
    request.user.main match {
      case user =>
        val redirectUrl = "https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_grant-permission&request_token="
        val sandboxUrl = "https://svcs.sandbox.paypal.com/Permissions/RequestPermissions"
        val baseUrl = current.configuration.getString("baseUrl").get
        val payload = Json.obj(
          "requestEnvelope" -> Json.obj(
            "errorLanguage" -> "en_US"),
          "scope" -> Json.arr("EXPRESS_CHECKOUT", "ACCESS_BASIC_PERSONAL_DATA"),
          "callback" -> s"$baseUrl/paypal/permcallback")
        paypalRequestHolder(sandboxUrl).post(payload).map { wsResponse =>
          val result = (wsResponse.json \ "responseEnvelope" \ "ack").as[String]
          if (result == "Success") {
            val requestToken = (wsResponse.json \ "token").as[String]
            Cache.set(key = requestToken, value = user.id.get.toString, expiration = 10.minutes)
            Ok(Json.obj(
              "url" -> s"$redirectUrl$requestToken",
              "status" -> "OK"))
          } else
            Unauthorized("Paypal authorization was unsuccessful")
        }
    }
  }

  def permCallback = Action.async { implicit request =>
    val getAccessUrl = "https://svcs.sandbox.paypal.com/Permissions/GetAccessToken"
    val baseUrl = current.configuration.getString("baseUrl").get
    val redirectUrl = s"$baseUrl/#!/userhome?id=0&tid=3"
    request.getQueryString("verification_code") match {
      case Some(verifier) =>
        val requestToken = request.getQueryString("request_token").get
        Cache.getAs[String](requestToken) match {
          case Some(userid) =>
            val payload = Json.obj(
              "requestEnvelope" -> Json.obj(
                "errorLanguage" -> "en_US"),
              "token" -> requestToken,
              "verifier" -> verifier)
            val paypalResponse = paypalRequestHolder(getAccessUrl).post(payload)
            val timeoutFuture = timeout("Oops", 15.seconds)
            Future.firstCompletedOf(Seq(paypalResponse, timeoutFuture)).map {
              case response: WSResponse =>
                val jsonResponse = response.json
                val token = (jsonResponse \ "token").as[String]
                val tokenSecret = (jsonResponse \ "tokenSecret").as[String]
                val scopeList = (jsonResponse \ "scope").as[Seq[String]]
                val scope = scopeList.foldLeft("") { case (acc, s) => if (acc.isEmpty) s else s"$acc&$s"}
                DB.withSession { implicit s =>
                  PaypalMerchants.insertOrUpdate(PaypalMerchant(
                      userid = userid.toLong,
                      token = token,
                      tokenSecret = tokenSecret,
                      scope = scope))
                  Redirect(redirectUrl)
                }
              case error: String => ServiceUnavailable(error)
            }
          case None =>
            Future.successful(Redirect(redirectUrl))
        }
      case None =>
        Future.successful(Redirect(redirectUrl))
    }
  }

  def deauthorizePaypal = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession{ implicit s =>
          PaypalMerchants.delete(user.id.get)

          // We need to add here a call to PayPal to cancel the permissions that were granted

          Ok(Json.obj("status" -> "OK", "message" -> "Paypal support has been removed"))
        }
    }
  }

  def paypalRequestHolder(url: String) = {
    val userid = current.configuration.getString("paypal.sandbox.userid").get
    val password = current.configuration.getString("paypal.sandbox.password").get
    val signature = current.configuration.getString("paypal.sandbox.signature").get
    val appId = current.configuration.getString("paypal.sandbox.appId").get
    WS.url(url).withHeaders(
        "X-PAYPAL-SECURITY-USERID" -> userid,
        "X-PAYPAL-SECURITY-PASSWORD" -> password,
        "X-PAYPAL-SECURITY-SIGNATURE" -> signature,
        "X-PAYPAL-REQUEST-DATA-FORMAT" -> "JSON",
        "X-PAYPAL-RESPONSE-DATA-FORMAT" -> "JSON",
        "X-PAYPAL-APPLICATION-ID" -> appId)
  }

  def paypalOAuthHeader(url: String, merchant: PaypalMerchant, method: OAuthSignature.HTTPMethod, queryParams: Map[_,_] = null) = {
    val userid = current.configuration.getString("paypal.sandbox.userid").get
    val password = current.configuration.getString("paypal.sandbox.password").get
    OAuthSignature.getFullAuthString(userid, password, merchant.token, merchant.tokenSecret, method, url, queryParams)
  }

}