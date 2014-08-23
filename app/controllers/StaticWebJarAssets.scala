package controllers

import play.api.mvc.Controller
import play.api.Play
import play.api.Play.current

object StaticWebJarAssets extends Controller {

  def at(file: String) = Assets.at("/META-INF/resources/webjars", file)

  def getUrl(file: String) = {
    val maybeContentUrl = Play.configuration.getString("contentUrl")

    maybeContentUrl.map { contentUrl =>
      contentUrl + controllers.routes.StaticWebJarAssets.at(file).url
    } getOrElse controllers.routes.StaticWebJarAssets.at(file).url
  }

}