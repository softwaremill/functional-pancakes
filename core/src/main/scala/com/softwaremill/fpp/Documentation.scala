package com.softwaremill.fpp

import sttp.tapir.asyncapi.circe.yaml._
import sttp.tapir.docs.asyncapi._

object Documentation extends App {
  val apiDocs =
    Server.pancakesEndpoint
      .toAsyncAPI("Functional pancakes", "1.0", List("dev" -> sttp.tapir.asyncapi.Server("localhost:8080", "ws")))
      .toYaml
  println(s"Paste into https://playground.asyncapi.io/ to see the docs for the pancakes endpoint:\n\n$apiDocs")
}
