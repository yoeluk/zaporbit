name := "ZapOrbit"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.3"

playScalaSettings

resolvers += "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies ++= {
  Seq(
    "com.typesafe.play" %% "play-slick" % "0.6.0.1",
    "mysql" % "mysql-connector-java" % "5.1.29",
    "org.cryptonode.jncryptor" % "jncryptor" % "1.0.1",
    "com.typesafe" %% "play-plugins-mailer" % "2.2.0",
    "commons-io" % "commons-io" % "2.3",
    "org.imgscalr" % "imgscalr-lib" % "4.2",
    "ws.securesocial" %% "securesocial" % "2.1.3",
    //"com.netaporter" %% "scala-uri" % "0.4.1",
    //"com.sksamuel.scrimage" %% "scrimage-core" % "1.3.20",
    //"com.sksamuel.scrimage" %% "scrimage-filters" % "1.3.20",
    cache,
    filters
    //"com.paypal.sdk" % "adaptivepaymentssdk" % "2.6.110"
  )
}