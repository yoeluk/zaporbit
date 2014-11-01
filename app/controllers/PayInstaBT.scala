package controllers

/**
 * Created by yoelusa on 30/10/14.
 */

import play.api.mvc._
import com.instabt._
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.concurrent.Promise.timeout
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object PayInstaBT extends Controller {

  def doPay = Action.async {

    val options = Some(Map(
      "url_call_back" -> "someUrlCallback",
      "url_success" -> "someUrlRedirect",
      "url_failure" -> "someUrlRedirect"
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

}
