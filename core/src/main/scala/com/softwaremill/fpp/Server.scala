package com.softwaremill.fpp

import com.typesafe.scalalogging.StrictLogging
import fs2.Pipe
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s._
import Json._

object Server extends StrictLogging {
  // a value which describes in detail a single query input
  val pansQueryInput: EndpointInput.Query[Int] = query[Int]("pans")
    .description("The number of frying pans to use in parallel")
    .example(2)
    .validate(Validator.min(1))

  // describing the web socket endpoint
  val pancakesEndpoint: Endpoint[Int, String, Pipe[Task, PancakeIngredient, PancakeStatus], WebSockets with Fs2Streams[Task]] =
    endpoint
      .in("pancakes")
      .in(pansQueryInput)
      .errorOut(stringBody)
      .out(webSocketBody[PancakeIngredient, CodecFormat.Json, PancakeStatus, CodecFormat.Json](Fs2Streams[Task]))

  // combining the endpoint description with server logic
  val pancakesServerEndpoint = pancakesEndpoint.serverLogic(ServerLogic.bakePancakes)

  // interpreting the combined endpoint+logic as http4s's HttpRoutes
  val pancakesServerRoutes: HttpRoutes[Task] = pancakesServerEndpoint.toRoutes

  def main(args: Array[String]): Unit = {
    // exposing the routes to the outside world, by binding to the given port/host
    BlazeServerBuilder[Task](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(Router("/" -> pancakesServerRoutes).orNotFound)
      .resource
      .use { _ => Task(logger.info("Server started")) >> Task.never }
      .runSyncUnsafe()
  }
}
