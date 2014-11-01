package controllers

import java.util

import com.paypal.core.credential.{SignatureCredential, TokenAuthorization}
import play.api._
import play.api.mvc._
import play.api.Play._
import play.api.libs.json._
import play.api.data._
import play.api.data.Mapping
import play.api.data.Forms._
import play.api.data.format._
import play.api.data.format.Formats._
import play.api.db.slick._
import play.api.Play.current

import com.paypal.svcs.services.AdaptivePaymentsService
import com.paypal.svcs.types.common.RequestEnvelope
import com.paypal.svcs.types.ap._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration._
import play.api.libs.concurrent.Promise.timeout

object PayPal extends Controller {

  def paySandbox = Action.async {

    val receiver = new util.ArrayList[Receiver]()
    val env = new RequestEnvelope()
    env.setErrorLanguage("en_US")
    val rec = new Receiver()
    rec.setAmount(2.0)
    rec.setEmail("yoeluk@gmail.com")
    receiver.add(rec)
    val receiverLst = new ReceiverList()

    val payRequest = new PayRequest()
    payRequest.setReceiverList(receiverLst)

    val customConfigurationMap = new util.HashMap[String, String]()
    customConfigurationMap.put("mode", "Sandbox")
    val adaptivePaymentsService = new AdaptivePaymentsService(customConfigurationMap)

    val thirdPartyAuth = new TokenAuthorization("accessToken", "tokenSecret")
    val cred = new SignatureCredential("jb-us-seller_api1.paypal.com", "WX4WTU3S8MY44S7F", "AFcWxV21C7fd0v3bYYYRCpSSRl31A7yDhhsPUU2XhtMoZXsWHFxu-RWy")
    cred.setApplicationId("APP-80W284485P519543T")
    cred.setThirdPartyAuthorization(thirdPartyAuth)

    val payResponse = Future(adaptivePaymentsService.pay(payRequest, cred))

    val timeoutFuture = timeout("Oops", 5.seconds)
    Future.firstCompletedOf(Seq(payResponse, timeoutFuture)).map {
      case response: PayResponse => Ok("Got result: " + response.getPayKey)
      case t: String => InternalServerError(t)
    }

  }
}