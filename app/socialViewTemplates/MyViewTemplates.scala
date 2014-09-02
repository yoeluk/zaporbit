package socialViewTemplates

import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc.{Controller, RequestHeader}
import play.twirl.api.Html
import securesocial.controllers.{ChangeInfo, RegistrationInfo, ViewTemplates}
import securesocial.core.RuntimeEnvironment

trait MyViewTemplates extends ViewTemplates {

  /**
   * Returns the html for embeded login
   */
  def getEmbededLoginPartial(form: Form[(String, String)], msg: Option[String] = None, redirect: Option[String] = None)(implicit request: RequestHeader, lang: Lang): Html
}

object MyViewTemplates {
  /**
   * The default views.
   */
  class Default(env: RuntimeEnvironment[_]) extends MyViewTemplates {

    implicit val implicitEnv = env

    def getLoginPage(form: Form[(String, String)],
                              msg: Option[String] = None)(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.login(form, msg)(request, lang, env)
    }

    def getEmbededLoginPartial(form: Form[(String, String)],
                              msg: Option[String] = None, redirect: Option[String] = None)(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.embededLogin(form, msg, redirect)(request, lang, env)
    }

    def getSignUpPage(form: Form[RegistrationInfo], token: String)(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.Registration.signUp(form, token)(request, lang, env)
    }

    def getStartSignUpPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.Registration.startSignUp(form)(request, lang, env)
    }

    def getStartResetPasswordPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.Registration.startResetPassword(form)(request, lang, env)
    }

    def getResetPasswordPage(form: Form[(String, String)], token: String)(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.Registration.resetPasswordPage(form, token)(request, lang, env)
    }

    def getPasswordChangePage(form: Form[ChangeInfo])(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.passwordChange(form)(request, lang, env)
    }

    def getNotAuthorizedPage(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.notAuthorized()(request, lang, env)
    }
  }
}