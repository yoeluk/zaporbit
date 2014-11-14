package controllers

import com.typesafe.plugin._
import play.api.libs.concurrent.Promise._
import play.api.mvc._
import play.api.libs.json._

import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._

import play.api.Play.current

import play.api.cache.Cache
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

import concurrent.duration._
import concurrent.Await

import xml._

/**
 * Created by yoelusa on 21/06/2014.
 */

case class Issue(summary: String, description: String, email: Option[String])

object Youtrack extends Controller {

  implicit val listingFormat = Json.format[Issue]
  val listingForm = Form(
    mapping(
      "summary" -> nonEmptyText,
      "description" -> nonEmptyText,
      "email" -> optional(of[String])
    )(Issue.apply)(Issue.unapply)
  )

  def cookieFromResponse(response: WSResponse) = {
    response.cookie("YTJSESSIONID") match {
      case Some(x1) =>
        response.cookie("jetbrains.charisma.main.security.PRINCIPAL") match {
          case Some(x2) =>
            val x2 = response.cookie("jetbrains.charisma.main.security.PRINCIPAL").get
            Cache.set("login.key1", x1.value.get, 1.day)
            Cache.set("login.key2", x2.value.get, 1.day)
            s"YTJSESSIONID=${x1.value.get}; jetbrains.charisma.main.security.PRINCIPAL${x2.value.get}"
        }
      case None =>
        println(response.cookies.toString())
        ""
    }
  }

  def allIssues = Action.async {
    val urlLogin = "http://youtrack.zaporbit.com/rest/user/login"
    val filter = java.net.URLEncoder.encode("visible to: {All Users}", "UTF-8")
    val url = s"http://youtrack.zaporbit.com/rest/issue/byproject/ZO?filter=$filter&max=25"
    Cache.getAs[String]("login.key1") match {
      case None =>
        val youtrackResponse = for {
          response <- WS.url(urlLogin).withHeaders("Accept" -> "application/json; charset=utf-8")
            .post(Map("login" -> Seq("yoeluk"), "password" -> Seq("qWerty.19")))
          issuesResponse <- WS.url(url).withHeaders(
            "Accept" -> "application/json; charset=utf-8",
            "Cookie" -> (cookieFromResponse(response))).get()
        } yield issuesResponse
        youtrackResponse.recover {
          case e: Exception => InternalServerError(e.getMessage)
          case _ => InternalServerError("unknow error")
        }
        val futureTimeout = timeout("Whoops", 15.seconds)
        Future.firstCompletedOf(Seq(youtrackResponse, futureTimeout)).map {
          case resp: WSResponse => Ok(Json.obj("issues" -> resp.json))
          case error: String => ServiceUnavailable(error)
        }
      case Some(x1_value) =>
        WS.url(url)
          .withHeaders("Accept" -> "application/json; charset=utf-8",
            "Cookie" -> ("YTJSESSIONID="+x1_value+"; jetbrains.charisma.main.security.PRINCIPAL"+Cache.get("login.key2")))
          .get().map { resp =>
          (resp.json \ "value").asOpt[String] match {
            case Some(msg) =>
              Cache.remove("login.key1")
              Cache.remove("login.key2")
              Redirect(routes.Youtrack.allIssues())
            case None =>
              Ok(Json.obj(
                "issues" -> resp.json)
              )
          }
        }
    }
  }

  def createIssue = Action.async(parse.json) { implicit request =>
    request.body.validate[Issue].map { issue =>
      issue.email match {
        case None =>
          val urlLogin = "http://youtrack.zaporbit.com/rest/user/login"
          val url = "http://youtrack.zaporbit.com/rest/issue"
          val respond = Await.result(WS.url(urlLogin)
            .withHeaders("Accept" -> "application/json; charset=utf-8")
            .post(Map("login" -> Seq("yoeluk"), "password" -> Seq("qWerty.19"))), 5.seconds)
          respond.cookie("YTJSESSIONID") match {
            case Some(x3) =>
              val x4 = respond.cookie("jetbrains.charisma.main.security.PRINCIPAL").get
              WS.url(url)
                .withHeaders("Accept" -> "application/json; charset=utf-8")
                .withHeaders("Cookie" -> ("YTJSESSIONID=" + x3.value.get + "; jetbrains.charisma.main.security.PRINCIPAL" + x4.value.get))
                .put(Map(
                  "project" -> Seq("ZO"),
                  "summary" -> Seq(issue.summary),
                  "description" -> Seq(issue.description),
                  "permittedGroup" -> Seq("developers"))
                ).map { resp =>
                Ok("new issue created")
              }
            case None =>
              Future.successful(InternalServerError("there was an error authenticating with youtrack@zaporbit"))
          }
        case _ =>
          val mail = use[MailerPlugin].email
          mail.setSubject(issue.summary)
          mail.setFrom(issue.email.get)
          mail.setRecipient("support@zaporbit.com")
          mail.send(issue.description)
          Future.successful(Ok("there was an error authenticating with youtrack@zaporbit"))
      }

    }.getOrElse {
      Future.successful(BadRequest("posting an invalid new issue"))
    }
  }

  def getStats = Action.async {
    val urlLogin = "http://youtrack.zaporbit.com/rest/user/login"
    val url = "http://youtrack.zaporbit.com/rest/issue/counts?rough=true"
    val queryData = xml.query.render()
    Cache.getAs[String]("login.key5") match {
      case None =>
        val respond = Await.result(WS.url(urlLogin)
          .withHeaders(
            "Accept" -> "application/json; charset=utf-8"/*,
            "Content-Type" -> "application/json; charset=utf-8"*/
          ).post(Map("login" -> Seq("yoeluk"), "password" -> Seq("qWerty.19"))), 5.seconds)
        respond.cookie("YTJSESSIONID") match {
          case Some(x5) =>
            val x6 = respond.cookie("jetbrains.charisma.main.security.PRINCIPAL").get
            Cache.set("login.key5", x5.value.get, 1.day)
            Cache.set("login.key6", x6.value.get, 1.day)
            WS.url(url)
              .withHeaders("Accept" -> "application/json; charset=utf-8")
              .withHeaders("Cookie" -> ("YTJSESSIONID="+x5.value.get+"; jetbrains.charisma.main.security.PRINCIPAL"+x6.value.get))
              .post(queryData).map { resp =>
              Ok(resp.json)
            }
          case None =>
            Future.successful(InternalServerError("there was an error authenticating with youtrack@zaporbit"))
        }
      case Some(x5_value) =>
        WS.url(url)
          .withHeaders("Accept" -> "application/json; charset=utf-8")
          .withHeaders("Cookie" -> ("YTJSESSIONID="+x5_value+"; jetbrains.charisma.main.security.PRINCIPAL"+Cache.get("login.key6")))
          .post(queryData).map { resp =>
          (resp.json \ "value").asOpt[String] match {
            case Some(msg) =>
              Cache.remove("login.key5")
              Cache.remove("login.key6")
              Redirect(routes.Youtrack.getStats())
            case None =>
              Ok(resp.json)
          }
        }
    }
  }

}
