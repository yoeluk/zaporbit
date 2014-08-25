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

  def getUrl2(file: String, ver: String) = {
    val maybeContentUrl = Play.configuration.getString("contentUrl")

    maybeContentUrl.map { contentUrl =>
      val parts = file.split("/").toList
      val verFile = (for {
        (part, i) <- parts.zipWithIndex
      } yield if (i == 1) ver else part).mkString("/")
      contentUrl + controllers.routes.StaticWebJarAssets.at(verFile).url
    } getOrElse controllers.routes.StaticWebJarAssets.at(file).url
  }

}