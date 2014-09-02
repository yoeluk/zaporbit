package controllers

import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.libs.json._
import securesocial.controllers.BaseLoginPage
import securesocial.core.providers.UsernamePasswordProvider

import securesocial.core.services.RoutesService
import securesocial.core.{RuntimeEnvironment, IdentityProvider}

import securesocial.core._
import service.SocialUser

import AppCryptor._
import play.api.data._
import play.api.data.Forms._
import socialViews.MyViewTemplates

class Application(override implicit val env: RuntimeEnvironment[SocialUser]) extends securesocial.core.SecureSocial[SocialUser] {

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
    } else if (partial == "profile") {
      Redirect( routes.CustomLoginController.embededLogin( "/#!/shopping" ) )
    } else {
      BadRequest(partial + " could not be found")
    }
  }

  def profileTemplate = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        Ok( partials.html.profile( user.fbuserid, user.name ) )
    }
  }

  def currentUser = Action.async { implicit request =>
    import play.api.libs.concurrent.Execution.Implicits._
    SecureSocial.currentUser[SocialUser].map { maybeUser =>
      val userId = maybeUser.map(_.main.fbuserid).getOrElse("unknown")
      Ok(s"Your id is $userId")
    }
  }

}

class CustomLoginController(implicit override val env: RuntimeEnvironment[SocialUser]) extends BaseLoginPage[SocialUser] {

  lazy val myViews = new MyViewTemplates.Default(env)

  override def login: Action[AnyContent] = {
    Logger.debug("using CustomLoginController")
    super.login
  }

  def embededLogin(redirect: String) = UserAwareAction { implicit request =>
    if ( request.user.isDefined ) {
      Redirect( routes.Application.profileTemplate() )
    } else {
      Ok( myViews.getEmbededLoginPage(form = UsernamePasswordProvider.loginForm, redirect = Some(redirect)) )
    }
  }

}

class CustomRoutesService extends RoutesService.Default {
  override def loginPageUrl(implicit req: RequestHeader): String = routes.CustomLoginController.login().absoluteURL(IdentityProvider.sslEnabled)
}