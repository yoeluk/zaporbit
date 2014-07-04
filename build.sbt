name := "ZapOrbit"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtTwirl)

resolvers += "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= {
  Seq(
    "com.typesafe.play" %% "play-slick" % "0.7.0",
    "mysql" % "mysql-connector-java" % "5.1.29",
    "org.cryptonode.jncryptor" % "jncryptor" % "1.0.1",
    "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0",
    "commons-io" % "commons-io" % "2.3",
    "org.imgscalr" % "imgscalr-lib" % "4.2",
    "ws.securesocial" %% "securesocial" % "master-SNAPSHOT",
    "com.googlecode.jsontoken" % "jsontoken" % "1.1",
    "com.google.code.gson" % "gson" % "2.2.4",
    "commons-codec" % "commons-codec" % "1.6",
    "com.google.collections" % "google-collections" % "1.0",
    "joda-time" % "joda-time" % "2.1",
    "com.googlecode.json-simple" % "json-simple" % "1.1.1",
    cache,
    filters,
    ws
  )
}