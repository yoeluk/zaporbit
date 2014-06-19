package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.libs.json._

import models._

import AppCryptor._
import play.api.data._
import play.api.data.Forms._

object Application extends Controller {

  // HOME PAGE
  case class UpgradeListing(offerid: Long, waggle: Boolean, highlight: Boolean)
  implicit val upgradeFormat = Json.format[UpgradeListing]
  val upgradeForm = Form(
    mapping(
      "offerid" -> longNumber,
      "waggle" -> boolean,
      "highlight" -> boolean
    )(UpgradeListing.apply)(UpgradeListing.unapply)
  )

  def index = Action {

    Ok(views.html.index(""))
  }

  def upgradeListing(tick: String) = DBAction(parse.raw) { implicit rs =>
    rs.request.body.asBytes(maxLength = 1024) match {
      case Some(body) =>
        val pass = password + tick
        val decryptedBody = appCryptor.decryptData(body, pass.toCharArray)
        Json.parse(decryptedBody).validate[UpgradeListing].map { upgrade =>
          Redirect(routes.Application.index())
        }.getOrElse(BadRequest(Json.obj(
          "status" -> "KO",
          "message" -> ""
        )))
      case None =>
        BadRequest("no post body found")
    }
  }
}