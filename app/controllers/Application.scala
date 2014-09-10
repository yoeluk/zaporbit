package controllers

import controllers.Wallet._
import models._
import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.libs.json._
import securesocial.controllers.BaseLoginPage
import securesocial.core.providers.UsernamePasswordProvider

import securesocial.core.services.RoutesService
import securesocial.core.{RuntimeEnvironment, IdentityProvider}

//import play.api.db.slick.Config.driver.simple._
//import play.api.db.slick.Config.driver.simple.{Session => DBSession}

import securesocial.core._
import service.SocialUser

import play.api.Play.current

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
  /***
  def calcRatingForUsers(page: List[(Listing, ZOLocation, User)])(implicit session: DBSession): Map[Long, (Int, Int)] = {
    val diffUsers = page.foldLeft(Nil: List[Long]) {
      (acc, next) => if (acc contains next._3.id.get) acc else next._3.id.get :: acc
    }
    val rtgs = (for {
      rt <- ratings.filter(_.id inSet diffUsers)
    } yield rt.userid -> rt.rating).toMap
    rtgs.foldLeft(Map(): Map[Long, (Int, Int)]) {
      case (a, (k, v)) => if (a contains k) adjust(a, k){case (v1,c) => (v+v1,c+1)} else a + (k -> (v, 1))
    }
  }
  ***/

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
                      Some(generateToken(offer, merchant.identifier, merchant.secret, user.id.get))
                  }
                } else None
                val lst = Offers.listingWithOffer(offer)
                val rt = Locations.calcRatingForUsers(List((lst, ZOLoc, user))).getOrElse(user.id.get, (5,1))
                val rating = 100*((50+rt._1)/(5*(rt._2+10))).toFloat
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
    } else if (partial == "profile") {
      request.headers.get("X-Auth-Token") match {
        case Some(_) =>
          Redirect( routes.Application.profileTemplate() )
        case None =>
          Redirect( routes.CustomLoginController.embededLogin( "/#!/listings" ) )
      }
    } else
        BadRequest(partial + " could not be found")
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