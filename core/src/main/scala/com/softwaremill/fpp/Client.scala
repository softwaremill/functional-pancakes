package com.softwaremill.fpp

import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import sttp.client3._
import sttp.client3.httpclient.monix.HttpClientMonixBackend
import sttp.ws.WebSocket
import monix.execution.Scheduler.Implicits.global
import io.circe.syntax._

import scala.concurrent.duration.DurationInt

object Client extends App with StrictLogging {
  def receiveAndLog(ws: WebSocket[Task]): Task[Unit] =
    ws.receiveText().flatMap {
      case Left(_)    => Task(logger.info("Web socket closed, bye!"))
      case Right(msg) => Task(logger.info("Received: " + msg)) >> receiveAndLog(ws) // recursive loop (stack safe)
    }

  def sendIngredient(ws: WebSocket[Task], ingredient: PancakeIngredient): Task[Unit] = {
    import Json._ // importing the codecs
    ws.sendText(ingredient.asJson.noSpaces)
  }

  def send(ws: WebSocket[Task]): Task[Unit] =
    sendIngredient(ws, Flour(100)) >>
      sendIngredient(ws, Flour(450)) >>
      sendIngredient(ws, Eggs(4)) >>
      sendIngredient(ws, Milk(1.5)) >>
      Task.sleep(10.seconds) >>
      sendIngredient(ws, Eggs(2)) >>
      sendIngredient(ws, Flour(300)) >>
      Task.sleep(10.seconds) >>
      Task(logger.info("Closing web socket, bye!"))

  def handlePancakesWebSocket(ws: WebSocket[Task]): Task[Unit] =
    for {
      receiveFiber <- receiveAndLog(ws).start // starting the receive log in the background
      _ <- send(ws) // sending ingredients in the parent fiber
      _ <- receiveFiber.cancel
    } yield ()

  HttpClientMonixBackend
    .resource()
    .use { backend =>
      val pans = 3
      basicRequest
        .get(uri"ws://localhost:8080/pancakes?pans=$pans")
        .response(asWebSocket(handlePancakesWebSocket))
        .send(backend)
    }
    .runSyncUnsafe()
}
