package controllers

/**
 * Created by yoelusa on 30/10/14.
 */

import play.api.libs.ws.WS
import play.api.mvc._
import com.instabt._
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.concurrent.Promise.timeout
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object PayInstaBT extends Controller {

  def currecyConverter(amount: String, currency: String) = Action.async {
    WS.url(s"https://www.google.com/finance/converter?a=$amount&from=$currency&to=BTC")
      .withHeaders(
        "Accept" -> "text/xml"
      ).get().map { response =>
      val startIndex = response.body indexOf "= <span class=bld>"
      val endIndex = response.body indexOf " BTC</span>"
      Ok(response.body.substring(startIndex+18, endIndex+4))
    }
  }

  def doTestPay = Action.async {

    val options = Some(Map(
      "url_success" -> "https://zaporbit.com/instabt/success",
      "url_failure" -> "https://zaporbit.com/instabt/failure"
    ))

    val key = current.configuration.getString("instaBT.key").get

    val secret = current.configuration.getString("instaBT.secret").get

    val config = Configuration(key = key, secret = secret, amount = 3.0, options = options)

    val instaResponse = InstaBT.payWithConfiguration(config)

    val timeoutResponse = timeout("Oops", 15.seconds)

    Future.firstCompletedOf( Seq(instaResponse, timeoutResponse) ).map {
      case payResponse: PayResponse => payResponse.payData match {
        case Some(payData) =>
          Redirect(payData.Url)
        case None =>
          ServiceUnavailable(payResponse.statusText)
      }
      case error: String => InternalServerError(error)
    }
  }

  def successfulPayment = Action {
    Ok("Great!")
  }

  def failedPayment = Action {
    Ok("Oops!")
  }

}
