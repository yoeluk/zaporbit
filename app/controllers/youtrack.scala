package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.db.slick._

import play.api.Play.current

import play.api.cache.Cache
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import concurrent.duration._
import concurrent.Await

/**
 * Created by yoelusa on 21/06/2014.
 */

object Youtrack extends Controller {

  def allIssues = Action.async { implicit request =>
    val urlLogin = "http://youtrack.zaporbit.com/rest/user/login"
    val url = "http://youtrack.zaporbit.com/rest/issue?all"
    Cache.getAs[String]("login.key1") match {
      case None =>
        val respond = Await.result(WS.url(urlLogin)
          .withHeaders("Accept" -> "application/json; charset=utf-8")
          .post(Map("login" -> Seq("appuser"), "password" -> Seq("qWerty.19"))), 5.seconds)
        respond.cookie("JSESSIONID") match {
          case Some(x1) =>
            val x2 = respond.cookie("jetbrains.charisma.main.security.PRINCIPAL").get
            Cache.set("login.key1", x1.value.get, 1.hour)
            Cache.set("login.key2", x2.value.get, 1.hour)
            WS.url(url)
              .withHeaders("Accept" -> "application/json; charset=utf-8")
              .withHeaders("Cookie" -> ("JSESSIONID="+x1.value.get+"; jetbrains.charisma.main.security.PRINCIPAL"+x2.value.get))
              .get().map { resp =>
              Ok(resp.json)
            }
          case None =>
            Future(InternalServerError("there was an error authenticating with youtrack@zaporbit"))
        }
      case Some(x1_value) =>
        WS.url(url)
          .withHeaders("Accept" -> "application/json; charset=utf-8")
          .withHeaders("Cookie" -> ("JSESSIONID="+x1_value+"; jetbrains.charisma.main.security.PRINCIPAL"+Cache.get("login.key2")))
          .get().map { resp =>
          (resp.json \ "value").asOpt[String] match {
            case Some(msg) =>
              Cache.remove("login.key1")
              Cache.remove("login.key2")
              Redirect(routes.Youtrack.allIssues())
            case None =>
              Ok(resp.json)
          }
        }
    }
  }

}
