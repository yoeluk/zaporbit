package controllers

import models._
import play.api._
import play.api.mvc._
import play.api.db.slick._
import play.api.libs.json._
import securesocial.controllers.BaseLoginPage

import play.api.db.slick.Config.driver._

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
            Users.findById(offer.userid) match {
              case Some(user) =>
                val optToken = if (user.isMerchant.get) {
                  Merchants.findByUserId(offer.userid) match {
                    case Some(merchant) =>
                      Some(Wallet.generateToken(offer, merchant.identifier, merchant.secret, user.id.get))
                  }
                } else None
                val lst = Offers.listingWithOffer(offer)
                val rt = Ratings.ratingForUser(user.id.get)
                val rating = 100*rt._1.toInt
                Ok(partials.html.itemTemplate(lst, lst.pictures.get, loc, user, rating, currency = offer.currency_code, token = optToken, pictureUrl(user.id.get)))
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

  def userProfile = DBAction { implicit rs =>
    rs.getQueryString("id") match {
      case Some(userid) =>
        Users.findById(userid.toLong) match {
          case Some(user) =>
            val rt = Ratings.ratingForUser(user.id.get)
            val rating = 100*rt._1.toInt
            val defaultOptions = UserOption(
              userid = user.id.get,
              background = Some("/vassets/images/profile_cover.png"),
              picture = Some("//graph.facebook.com/v2.1/"+user.fbuserid+"/picture?height=200&width=200"),
              about = Some("Tell others a little bit about you in one sentence. What is worth your while?"))
            UserOptions.findByUserid(user.id.get) match {
              case None =>
                Ok( partials.html.userProfile( user, rating, defaultOptions ) )
              case Some(opts) =>
                val currentOptions = UserOption(
                  userid = opts.userid,
                  background = opts.background match {
                    case None =>
                      Some("/vassets/images/profile_cover.png")
                    case Some(b) => Some("/options/pictures/"+b)
                  },
                  picture = opts.picture match {
                    case None =>
                      Some("/vassets/images/pic_placeholder.png")
                    case Some(p) => Some("/options/pictures/"+p)
                  },
                  about = opts.about match {
                    case None =>
                      Some("Tell others a little bit about you in one sentence. What is worth your while?")
                    case x => x
                  }
                )
                Ok( partials.html.userProfile( user, rating, currentOptions ) )
            }
          case None =>
            BadRequest("")
        }
      case None =>
        BadRequest("no id param found")
    }

  }

  def partialTemplates(partial: String) = Action { implicit request =>
    if ( partial == "home" ) {
      Ok(partials.html.home("") )
    } else if ( partial == "support" ) {
      Ok(partials.html.support("") )
    } else if ( partial == "listings" ) {
      Ok(partials.html.shopping("") )
    } else if ( partial == "modalItem" ) {
      Ok(partials.html.uiModalItem() )
    } else if ( partial == "loginPartial" ) {
      Ok( partials.html.loginPartial() )
    } else if ( partial == "userhome" ) {
      Ok( partials.html.userhome() )
    } /*else if ( partial == "userprofile" ) {
      request.getQueryString("id") match {
        case Some( userid ) =>
          Redirect( routes.Application.userProfile( userid.toLong ) )
        case None =>
          BadRequest("unknown user")
      }
    } */else if (partial == "profile") {
      request.headers.get("X-Auth-Token") match {
        case Some(_) =>
          Redirect( routes.Application.profileTemplate() )
        case None =>
          Redirect( routes.Application.loggedoutTemplate() )
      }
    } else if (partial == "profileprofile") {
      request.headers.get("X-Auth-Token") match {
        case Some(_) =>
          Redirect( routes.Application.profileProfile() )
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
          val defaultPictureUrl = "//graph.facebook.com/v2.1/"+user.fbuserid+"/picture?height=200&width=200"
          UserOptions.findByUserid(user.id.get) match {
            case None =>
              Ok( partials.html.profile( user, rating, defaultPictureUrl ) )
            case Some(opts) =>
              val customPictureUrl = opts.picture match {
                  case None => defaultPictureUrl
                  case Some(b) => "/options/pictures/"+b
                }
              Ok( partials.html.profile( user, rating, customPictureUrl ) )
          }
        }
    }
  }

  def profileProfile = SecuredAction { implicit request =>
    request.user.main match {
      case user =>
        DB.withSession { implicit s =>
          val rt = Ratings.ratingForUser(user.id.get)
          val rating = 100*rt._1.toInt
          val defaultOptions = UserOption(
            userid = user.id.get,
            background = Some("/vassets/images/profile_cover.png"),
            picture = Some("/vassets/images/pic_placeholder.png"),
            about = Some("Tell others a little bit about you in one sentence. What is worth your while?"))
          UserOptions.findByUserid(user.id.get) match {
            case None =>
              Ok( partials.html.profileProfile( user, rating, defaultOptions ) )
            case Some(opts) =>
              val currentOptions = UserOption(
                userid = opts.userid,
                background = opts.background match {
                  case None =>
                    Some("/vassets/images/profile_cover.png")
                  case Some(b) => Some("/options/pictures/"+b)
                },
                picture = opts.picture match {
                  case None =>
                    Some("/vassets/images/pic_placeholder.png")
                  case Some(p) => Some("/options/pictures/"+p)
                },
                about = opts.about match {
                  case None =>
                    Some("Tell others a little bit about you in one sentence. What is worth your while?")
                  case x => x
                }
              )
              Ok( partials.html.profileProfile( user, rating, currentOptions ) )
          }
        }
    }
  }

  def credentialTemplate(userid: String) = DBAction { implicit rs =>
    Ok(partials.html.credentials( pictureUrl( userid.toLong ) ))
  }

  def pictureUrl(userid: Long)(implicit s: simple.Session): String = {
    UserOptions.findByUserid(userid.toLong) match {
      case None =>
        "//graph.facebook.com/v2.1/{{lst.user.fbuserid}}/picture?height=200&width=200"
      case Some(opts) =>
        val parts = opts.picture.get.split("\\.")
        if (parts.length > 1) "/options/pictures/" + opts.picture.get
        else "/options/pictures/" + opts.picture.get + ".jpg"
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