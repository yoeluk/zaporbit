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

    //Cache.set("login.key", cookies.head, 3600)

    val urlLogin = "http://youtrack.zaporbit.com/rest/user/login?appuser&r4Tty0-Pw"
    val url = "http://youtrack.zaporbit.com/rest/issue?all"
    WS.url(url)
      .withHeaders("Accept" -> "application/json; charset=utf-8")
      .get().map { resp =>
      Ok(resp.json)
    }

  }

  def allIssues1 = Action.async { implicit request =>

    val urlLogin = "http://youtrack.zaporbit.com/rest/user/login"
    val url = "http://youtrack.zaporbit.com/rest/issue?all"

    Cache.getAs[String]("login.key1") match {
      case None =>
        val respond = Await.result(WS.url(urlLogin)
          .withHeaders("Accept" -> "application/json; charset=utf-8")
          .post(Map("login" -> Seq("appuser"), "password" -> Seq("qWerty.19"))), 10.seconds)
        respond.cookie("JSESSIONID") match {
          case Some(x1) =>
            val x2 = respond.cookie("jetbrains.charisma.main.security.PRINCIPAL").get
            Cache.set("login.key1", x1.value.get, 1.hour)
            Cache.set("login.key2", x2.value.get, 1.hour)
            println(respond.cookie("JSESSIONID"))
            println(respond.cookie("jetbrains.charisma.main.security.PRINCIPAL"))
            WS.url(url)
              .withHeaders("Accept" -> "application/json; charset=utf-8")
              .withHeaders("Cookie" -> ("JSESSIONID="+x1.value.get+"; jetbrains.charisma.main.security.PRINCIPAL"+x2.value.get))
              .get().map { resp =>
              //println(resp.json)
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
              //println(msg)
              Cache.remove("login.key1")
              Cache.remove("login.key2")
              Ok(resp.json)
            case None =>
              //println("logged in with cached values")
              Ok(resp.json)
          }
        }
    }
  }

}
