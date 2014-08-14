import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter

import controllers.CustomRoutesService
import java.lang.reflect.Constructor
import securesocial.controllers.ViewTemplates
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers.FacebookProvider
import service.{SocialUser, MyEventListener, SocialUserService}

import scala.collection.immutable.ListMap

object Global extends WithFilters(new GzipFilter()) with play.api.GlobalSettings {

  /**
   * The runtime environment for this sample app.
   */
  object MyRuntimeEnvironment extends RuntimeEnvironment.Default[SocialUser] {
    override lazy val routes = new CustomRoutesService()
    override lazy val userService = new SocialUserService()
    override lazy val eventListeners = List(new MyEventListener())
    override lazy val viewTemplates: ViewTemplates = new MyViewTemplates.Default(this)
    override lazy val providers = ListMap(
      // oauth 2 client providers
      include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook)))
    )
  }
  /**
   * An implementation that checks if the controller expects a RuntimeEnvironment and
   * passes the instance to it if required.
   *
   *
   *
   * This can be replaced by any DI framework to inject it differently.
   *
   * @param controllerClass class
   * @tparam A type A
   * @return
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    val instance = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[RuntimeEnvironment[SocialUser]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(MyRuntimeEnvironment)
    }
    instance.getOrElse(super.getControllerInstance(controllerClass))
  }

}