import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.SbtWeb
import play.PlayScala

name := "ZapOrbit"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.2"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

resolvers += "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= {
  Seq(
    jdbc,
    "com.typesafe.play" %% "play-slick" % "0.8.0",
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
    "org.webjars" %% "webjars-play" % "2.3.0",
    "org.webjars" % "jquery" % "2.1.1",
    "org.webjars" % "jquery-ui" % "1.11.1",
    "org.webjars" % "bootstrap" % "3.2.0",
    "org.webjars" % "angularjs" % "1.3.0-rc.3",
    "org.webjars" % "angular-ui-bootstrap" % "0.11.2" exclude("org.webjars", "angularjs"),
    "org.webjars" % "textAngular" % "1.2.0" exclude("org.webjars", "angularjs"),
    //"org.webjars" % "holderjs" % "2.3.0",
    //"org.webjars" % "lz-string" % "1.3.3",
    "org.webjars" % "angular-moment" % "0.6.2-2" exclude("org.webjars", "angularjs"),
    "org.webjars" % "angular-google-maps" % "1.2.2" exclude("org.webjars", "angularjs"),
    "org.webjars" % "angular-ui-sortable" % "0.12.11-1" exclude("org.webjars", "jquery-ui"),
    "org.webjars" % "font-awesome" % "4.2.0",
    "org.webjars" % "angular-file-upload" % "1.6.7",
    cache,
    filters,
    ws
  )
}

Concat.groups := Seq(
  "main.css" -> group(Seq(
    "stylesheets/customNavbar.css",
    "stylesheets/app.css"
  )),
  "all.js" -> group(Seq(
    "javascripts/angular-local-storage.js",
    "javascripts/app.js",
    "javascripts/services.js",
    "javascripts/controllers.js",
    "javascripts/filters.js",
    "javascripts/directives.js"
  ))
  //"style-group2.css" -> group((sourceDirectory.value / "assets" / "style") * "*.css")
)

Concat.parentDir := "concated"

Closure.suffix := ".min.js"

Closure.flags := Seq("--formatting=PRETTY_PRINT", "--accept_const_keyword")

// Asset pipeline tasks
pipelineStages in Assets := Seq(concat)

includeFilter in closure := "all.js"

pipelineStages := Seq(closure, digest, gzip)

includeFilter in (Assets, LessKeys.less) := "star-rating.less"

LessKeys.compress in Assets := true

doc in Compile <<= target.map(_ / "none")

//compile in Compile <<= compile in Compile map {
//  comp =>
//    import scala.sys.process._
//    val exitCode = Seq("ssh", "-fNg", "-L", "3307:127.0.0.1:3306", "yoeluk@zaporbit.com")
//    println("exit code: " + exitCode)
//    comp
//}

scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code",
  "-language:reflectiveCalls"
)