val http4sVersion = "0.21.8"
val circeVersion = "0.13.0"
val sttpVersion = "3.0.0-RC7"
val tapirVersion = "0.17.0-M5"

lazy val commonSettings = commonSmlBuildSettings ++ ossPublishSettings ++ Seq(
  organization := "com.softwaremill.fpp",
  scalaVersion := "2.13.3"
)

val scalaTest = "org.scalatest" %% "scalatest" % "3.2.2" % Test

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "functional-pancakes")
  .aggregate(core)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "com.softwaremill.sttp.client3" %% "httpclient-backend-monix" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "slf4j-backend" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "circe" % sttpVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-asyncapi-docs" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-asyncapi-circe-yaml" % tapirVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      scalaTest
    )
  )
