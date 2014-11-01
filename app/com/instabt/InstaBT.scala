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

case class Configuration(key: String,
                         secret: String,
                         amount: Double,
                         currency: String = "BTC",
                         options: Option[Map[String, String]] = None)

case class PayData(Id: String,
                   Url: String,
                   Total: String,
                   Currency: String,
                   BtcRequired: String,
                   Data: String,
                   CreatedOn: String,
                   ExpiresOn: String,
                   LastUpdate: String,
                   Status: String)

case class PayResponse(status: Int,
                       statusText: String,
                       payData: Option[PayData] = None)

object InstaBT {

  val payDataForm = Form(
    mapping(
      "Id" -> text,
      "Url" -> text,
      "Total" -> text,
      "Currency" -> text,
      "BtcRequired" -> text,
      "Data" -> text,
      "CreatedOn" -> text,
      "ExpiresOn" -> text,
      "LastUpdate" -> text,
      "Status" -> text
    )(PayData.apply)(PayData.unapply)
  )

  def payWithConfiguration(config: Configuration,
                    end_point: String = "/create_order",
                    url: String = "https://api.instabt.com"): Future[PayResponse] = {

//    val rnd = new scala.util.Random
//    def tailWithSize(alphabet: String = "0123456789")(n: Int): String =
//      Stream.continually(rnd.nextInt(alphabet.size)).map(alphabet).take(n).mkString
//    def microTail = tailWithSize()(3)

    val utcTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val nonce = utcTime.getTimeInMillis.toString + "000"
    val payload =
      "amount=" + config.amount.toString +
      "&currency=" + config.currency +
      "&nonce=" + nonce +
      (config.options match {
        case Some(options) =>
          options.map { case (key, value) => s"&$key=$value" }.foldLeft("") { _ + _ }
        case None => ""
      })
    val sigData = end_point + '\u0000' + payload
    val mac = Mac.getInstance("HmacSHA512")
    val secretKey = new SecretKeySpec(config.secret.getBytes("UTF-8"), mac.getAlgorithm)
    val sign = {
      mac.init(secretKey)
      val byteString = mac.doFinal(sigData.getBytes).map { "%02x".format(_) }.foldLeft("") { _ + _ }
      Base64.encodeBase64String(byteString.getBytes)
    }
    WS.url(url+end_point).withHeaders(
        "API-KEY" -> config.key,
        "API-SIGN" -> sign
      ).post(payload).map { wsResponse =>
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
