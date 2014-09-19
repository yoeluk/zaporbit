package controllers

import models._
import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.libs.json._
import securesocial.controllers.BaseLoginPage

import securesocial.core.services.RoutesService
import securesocial.core.{RuntimeEnvironment, IdentityProvider}

import securesocial.core._
import service.SocialUser

import play.api.Play.current

class Application(override implicit val env: RuntimeEnvironment[SocialUser]) extends securesocial.core.SecureSocial[SocialUser] {

  // HOME PAGE

  def index = Action {
    val message = "Entice with higher confidence!"
    Ok(views.html.index(message))
  }

  def getListing(itemid: Long) = DBAction { implicit rs =>
    Offers.findById(itemid) match {
      case Some(offer) =>
        Locations.findZLocByOfferId(itemid) match {
          case Some(loc) =>
            val ZOLoc = ZOLocation(
              street = loc.street,
              locality = loc.locality,
              administrativeArea = loc.administrativeArea,
              latitude = loc.latitude,
              longitude = loc.longitude
            )
            Users.findById(offer.userid) match {
              case Some(user) =>
                val optToken = if(user.isMerchant.get) {
                  Merchants.findByUserId(offer.userid) match {
                    case Some(merchant) =>
                      Some(Wallet.generateToken(offer, merchant.identifier, merchant.secret, user.id.get))
                  }
                } else None
                val lst = Offers.listingWithOffer(offer)
                val rt = Ratings.ratingForUser(user.id.get)
                val rating = 100*rt._1.toInt
                Ok(partials.html.itemTemplate(lst, lst.pictures.get, loc, user, rating, currency = offer.currency_code, token = optToken))
              case None =>
                Ok(Json.obj(
                  "status" -> "KO"
                ))
            }
          case None =>
            Ok(Json.obj(
              "status" -> "KO"
            ))
        }
      case None =>
        Ok(Json.obj(
          "status" -> "KO"
        ))
    }
  }

  def partialTemplates(partial: String) = Action { implicit request =>
    if (partial == "home") {
      Ok(partials.html.home(""))
    } else if (partial == "support") {
      Ok(partials.html.support(""))
    } else if (partial == "listings") {
      Ok(partials.html.shopping(""))
    } else if (partial == "modalItem") {
      Ok(partials.html.uiModalItem())
    } else if (partial == "loginPartial") {
      Ok( partials.html.loginPartial() )
    } else if (partial == "userhome") {
      Ok( partials.html.userhome())
    } else if (partial == "profile") {
      request.headers.get("X-Auth-Token") match {
        case Some(_) =>
          Redirect( routes.Application.profileTemplate() )
        case None =>
          Redirect( routes.Application.loggedoutTemplate() )
      }
    } else
        BadRequest(partial + " could not be found")
  }

  def profileTemplate = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          val rt = Ratings.ratingForUser(user.id.get)
          val rating = 100*rt._1.toInt
          Ok( partials.html.profile( user, rating ) )
        }
    }
  }

  def loggedoutTemplate = Action {
    Ok( partials.html.loggedoutTemplate() )
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

  override def login: Action[AnyContent] = {
    Logger.debug("using CustomLoginController")
    super.login
  }

}

class CustomRoutesService extends RoutesService.Default {
  override def loginPageUrl(implicit req: RequestHeader): String = routes.CustomLoginController.login().absoluteURL(IdentityProvider.sslEnabled)
}