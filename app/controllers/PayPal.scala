package controllers

import com.paypal.sdk.util.OAuthSignature
import java.util. { Map => JMap}
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

  def paySuccess = Action {
    Ok("called back")
  }

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
                      val baseUrl = current.configuration.getString("baseUrl").get
                      val contact = PaypalContants.findByMerchantid(merchant.id.get).get
                      val payload = Json.obj(
                        "requestEnvelope" -> Json.obj(
                          "errorLanguage" ->"en_US"),
                        "actionType" -> "PAY",
                        "returnUrl" -> s"$baseUrl/paypal/paysuccess",
                        "cancelUrl" -> s"$baseUrl/#!/listing_item/${goods.listing.id.get}",
                        "currencyCode" -> goods.listing.currency_code,
                        "receiverList" -> Json.obj(
                          "receiver" -> Json.arr(Json.obj(
                            "email" -> contact.email,
                            "amount" -> goods.listing.price))))
                      val method = OAuthSignature.HTTPMethod.POST
                      val payResponse = paypalRequestHolder(url).withHeaders(
                          "X-PAYPAL-REQUEST-DATA-FORMAT" -> "JSON",
                          "X-PAYPAL-RESPONSE-DATA-FORMAT" -> "JSON",
                          "X-PAYPAL-DEVICE-IPADDRESS" -> request.remoteAddress,
                          "X-PAYPAL-AUTHORIZATION" -> paypalOAuthHeader(url = url, merchant = merchant, method = method)
                        ).post(payload)
                      val timeoutFuture = timeout("Whoops", 15.seconds)
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
        paypalRequestHolder(sandboxUrl).withHeaders("X-PAYPAL-REQUEST-DATA-FORMAT" -> "JSON", "X-PAYPAL-RESPONSE-DATA-FORMAT" -> "JSON")
          .post(payload).map { wsResponse =>
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
            val contactUrl = "https://svcs.sandbox.paypal.com/Permissions/GetBasicPersonalData"
            val method = OAuthSignature.HTTPMethod.POST
            val attrs = List(
              "attributeList.attribute(0)=http://axschema.org/contact/email",
              "attributeList.attribute(1)=http://axschema.org/namePerson/first",
              "attributeList.attribute(2)=http://axschema.org/namePerson/last",
              "attributeList.attribute(3)=http://axschema.org/company/name",
              "attributeList.attribute(4)=http://axschema.org/contact/country/home",
              "attributeList.attribute(5)=https://www.paypal.com/webapps/auth/schema/payerID",
              "requestEnvelope.errorLanguage=en_US")
              val paypalResponse = for {
                response <- paypalRequestHolder(getAccessUrl)
                  .withHeaders("X-PAYPAL-REQUEST-DATA-FORMAT" -> "JSON", "X-PAYPAL-RESPONSE-DATA-FORMAT" -> "JSON")
                  .post(payload)
                contactResponse <- paypalRequestHolder(contactUrl)
                  .withHeaders(
                    "X-PAYPAL-REQUEST-DATA-FORMAT" -> "NV",
                    "X-PAYPAL-RESPONSE-DATA-FORMAT" -> "JSON",
                    "X-PAYPAL-AUTHORIZATION" -> paypalOAuthHeader(
                      url = contactUrl,
                      merchant = merchantWithResponse(
                        response = response,
                        userid = userid),
                      method = method))
                  .post(attrs.foldLeft("") { case (acc, s) => if (acc.isEmpty) s else s"$acc&$s"})
              } yield contactResponse
              paypalResponse.recover {
                case e: Exception => InternalServerError(e.getMessage)
                case _ => InternalServerError("unknow error")
              }
              val contactTimeout = timeout("Whoops", 15.seconds)
              Future.firstCompletedOf(Seq(paypalResponse, contactTimeout)).map {
                case contact: WSResponse =>
                  DB.withSession { implicit s =>
                    (contact.json \ "responseEnvelope").asOpt[JsValue] match {
                      case Some(responseEnvelope) =>
                        if ((responseEnvelope \ "ack").as[String] == "Success") {
                          val personalData = (contact.json \ "response" \ "personalData").as[Seq[JsValue]].map { js => ((js \ "personalDataKey").as[String], (js \ "personalDataValue").as[String])}.toMap
                          PaypalMerchants.findByUserid(userid.toLong) match {
                            case Some(m) =>
                              PaypalContants.insertOrUpdate(PaypalContant(
                                merchantid = m.id.get,
                                name = personalData.getOrElse("http://axschema.org/namePerson/first", ""),
                                surname = personalData.getOrElse("http://axschema.org/namePerson/last", ""),
                                email = personalData.getOrElse("http://axschema.org/contact/email", ""),
                                businessName = personalData.get("http://axschema.org/company/name"),
                                country = personalData.getOrElse("http://axschema.org/contact/country/home", ""),
                                paypalid = personalData.getOrElse("", "https://www.paypal.com/webapps/auth/schema/payerID")))
                              Redirect(redirectUrl)
                            case None =>
                              PaypalMerchants.delete(userid.toLong)
                              Redirect(redirectUrl)
                          }
                        } else PaypalMerchants.delete(userid.toLong)
                        Redirect(redirectUrl)
                      case None =>
                        PaypalMerchants.delete(userid.toLong)
                        Redirect(redirectUrl)
                    }
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

  def merchantWithResponse(response: WSResponse, userid: String) = {
    val jsonResponse = response.json
    val token = (jsonResponse \ "token").as[String]
    val tokenSecret = (jsonResponse \ "tokenSecret").as[String]
    val scopeList = (jsonResponse \ "scope").as[Seq[String]]
    val scope = scopeList.foldLeft("") { case (acc, s) => if (acc.isEmpty) s else s"$acc&$s"}
    DB.withSession { implicit s =>
      val merchant = PaypalMerchant(
        userid = userid.toLong,
        token = token,
        tokenSecret = tokenSecret,
        scope = scope)
      PaypalMerchants.insertOrUpdate(merchant)
      merchant
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
    WS.url(url).withHeaders(
        "X-PAYPAL-SECURITY-USERID" -> current.configuration.getString("paypal.sandbox.userid").get,
        "X-PAYPAL-SECURITY-PASSWORD" -> current.configuration.getString("paypal.sandbox.password").get,
        "X-PAYPAL-SECURITY-SIGNATURE" -> current.configuration.getString("paypal.sandbox.signature").get,
        "X-PAYPAL-APPLICATION-ID" -> current.configuration.getString("paypal.sandbox.appId").get)
  }

  def paypalOAuthHeader(url: String, merchant: PaypalMerchant, method: OAuthSignature.HTTPMethod, queryParams: JMap[_,_] = null) = {
    val userid = current.configuration.getString("paypal.sandbox.userid").get
    val password = current.configuration.getString("paypal.sandbox.password").get
    OAuthSignature.getFullAuthString(userid, password, merchant.token, merchant.tokenSecret, method, url, queryParams)
  }

}