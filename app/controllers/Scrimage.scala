package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

object Scrimage extends Controller {

  def thumbnailForImage(imageName: String) = Action {
    Ok("").withHeaders(
      CONTENT_TYPE -> "application/octet-stream",
      CONTENT_DISPOSITION -> "attachment; filename=foo.txt"
    )
  }
}