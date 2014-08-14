import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc.{Controller, RequestHeader}
import play.twirl.api.Html
import securesocial.controllers.{ChangeInfo, RegistrationInfo, ViewTemplates}
import securesocial.core.RuntimeEnvironment

trait MyViewTemplates extends Controller {
  /**
   * Returns the html for the login page
   */
  def getLoginPage(form: Form[(String, String)], msg: Option[String] = None)(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the signup page
   */
  def getSignUpPage(form: Form[RegistrationInfo], token: String)(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the start signup page
   */
  def getStartSignUpPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the reset password page
   */
  def getResetPasswordPage(form: Form[(String, String)], token: String)(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the start reset page
   */
  def getStartResetPasswordPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the change password page
   */
  def getPasswordChangePage(form: Form[ChangeInfo])(implicit request: RequestHeader, lang: Lang): Html

  /**
   * Returns the html for the not authorized page
   */
  def getNotAuthorizedPage(implicit request: RequestHeader, lang: Lang): Html
}

object MyViewTemplates {
  /**
   * The default views.
   */
  class Default(env: RuntimeEnvironment[_]) extends ViewTemplates {
    implicit val implicitEnv = env

    override def getLoginPage(form: Form[(String, String)],
                              msg: Option[String] = None)(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.login(form, msg)(request, lang, env)
    }

    override def getSignUpPage(form: Form[RegistrationInfo], token: String)(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.Registration.signUp(form, token)(request, lang, env)
    }

    override def getStartSignUpPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.Registration.startSignUp(form)(request, lang, env)
    }

    override def getStartResetPasswordPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.Registration.startResetPassword(form)(request, lang, env)
    }

    override def getResetPasswordPage(form: Form[(String, String)], token: String)(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.Registration.resetPasswordPage(form, token)(request, lang, env)
    }

    override def getPasswordChangePage(form: Form[ChangeInfo])(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.passwordChange(form)(request, lang, env)
    }

    def getNotAuthorizedPage(implicit request: RequestHeader, lang: Lang): Html = {
      views.html.custom.notAuthorized()(request, lang, env)
    }
  }
}