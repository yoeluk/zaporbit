package com.instabt

/**
 * Created by yoelusa on 30/10/14.
 */

import org.apache.commons.codec.binary.Base64
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.ws._
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import java.util.TimeZone
import java.util.Calendar
import play.api.Play.current
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
/**
 * A configuration case class.
 * @param key Api key.
 * @param secret Api secret.
 * @param options The options. See the available options in the InstaBT documentation. It defaults to None if the field is not supplied.
 */
case class Configuration(key: String,
                         secret: String,
                         options: Option[Map[String, String]] = None)
/**
 * A paydata case class. Its fields match those in the json response of InstaBT
 * @param Id An order id.
 * @param Url A url for redirecting the browser.
 * @param Total The amount of the bill.
 * @param Currency The currency of the bill.
 * @param BtcRequired The amount of BTC required to fulfillment.
 * @param Data The data supplied with the request.
 * @param CreatedOn The created date.
 * @param ExpiresOn The expiry date.
 * @param LastUpdate The updated data.
 * @param Status The status of the transaction.
 */
case class PayData(Id: String,
                   Url: String,
                   Total: String,
                   Currency: String,
                   BtcRequired: String,
                   BtcAddress: Option[String],
                   Data: String,
                   CreatedOn: String,
                   ExpiresOn: String,
                   LastUpdate: String,
                   Status: String)
/**
 * A payresponse case class to return to the controller.
 * @param status An http status code.
 * @param statusText An http status text.
 * @param payData An optional PayData. It defaults to None if the field is not supplied
 */
case class PayResponse(status: Int,
                       statusText: String,
                       payData: Option[PayData] = None)
object InstaBT {
  /**
   * A paydata form to bind from the InstaBT response.json
   */
  val payDataForm = Form(
    mapping(
      "Id" -> text,
      "Url" -> text,
      "Total" -> text,
      "Currency" -> text,
      "BtcRequired" -> text,
      "BtcAddress" -> optional(nonEmptyText),
      "Data" -> text,
      "CreatedOn" -> text,
      "ExpiresOn" -> text,
      "LastUpdate" -> text,
      "Status" -> text
    )(PayData.apply)(PayData.unapply)
  )
  /**
   * Compose the request with the configuration param and make the call. It provides default values for url and end_point
   * @param config A configuration for this transaction.
   * @param end_point The services end point. It defaults to "/create_order" if the parameter is not supplied.
   * @param baseUrl The base url for the request. It default to "https://api.instabt.com" if the parameter is not supplied.
   * @return
   */
  def instaBTWithConfiguration(config: Configuration,
                           end_point: String = "/create_order",
                           baseUrl: String = "https://api.instabt.com"): Future[PayResponse] = {

//    val rnd = new scala.util.Random
//    def tailWithSize(alphabet: String = "0123456789")(n: Int): String =
//      Stream.continually(rnd.nextInt(alphabet.size)).map(alphabet).take(n).mkString
//    def microTail = tailWithSize()(3)

    val utcTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    // Until I know of a reliable way to get the time in microseconds in the jvm this should suffice.
    val nonce = s"${utcTime.getTimeInMillis}000"
    val payload = s"nonce=$nonce" +
      (config.options match {
        case Some(options) =>
          options.foldLeft("") { case (acc, (key, value)) => s"$acc&$key=$value" }
        case None => ""
      })
    val sigData = end_point + '\u0000' + payload
    val mac = Mac.getInstance("HmacSHA512")
    val secretKey = new SecretKeySpec(config.secret.getBytes("UTF-8"), mac.getAlgorithm)
    val signature = {
      mac.init(secretKey)
      val byteString = mac.doFinal(sigData.getBytes).foldLeft("") { case (acc, b) => s"$acc%02x".format(b) }
      Base64.encodeBase64String(byteString.getBytes)
    }
    WS.url(baseUrl+end_point)
      .withHeaders("API-KEY" -> config.key, "API-SIGN" -> signature)
      .post(payload).map { wsResponse =>
      if (wsResponse.status == 200) {
        PayResponse(
          status = wsResponse.status,
          statusText = wsResponse.statusText,
          payData = Some(payDataForm.bind(wsResponse.json).get)
        )
      } else
        PayResponse(
          status = wsResponse.status,
          statusText = wsResponse.statusText
        )
    }

  }

}
