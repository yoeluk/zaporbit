package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.libs.json._
import securesocial.controllers.BaseLoginPage

import securesocial.core.services.RoutesService
import securesocial.core.{RuntimeEnvironment, IdentityProvider}

import models._
import service.SocialUser
import views.html
import partials._

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
    val message = "Entice with higher confidence!"
    Ok(views.html.index(message))
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

  def partialTemplates(partial: String) = Action {
    if (partial == "home") {
      Ok(partials.html.home(""))
    } else if (partial == "support") {
      Ok(partials.html.support(""))
    } else if (partial == "shopping") {
      Ok(partials.html.shopping(""))
    } else {
      BadRequest(partial + " could not be found")
    }
  }

}

class CustomLoginController(implicit override val env: RuntimeEnvironment[SocialUser]) extends BaseLoginPage[SocialUser] {
  override def login: Action[AnyContent] = {
    Logger.debug("using CustomLoginController")
    super.login
  }
}

class CustomRoutesService extends RoutesService.Default {
  override def loginPageUrl(implicit req: RequestHeader): String = controllers.routes.CustomLoginController.login().absoluteURL(IdentityProvider.sslEnabled)
}