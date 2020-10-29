package com.softwaremill.fpp

import com.typesafe.scalalogging.StrictLogging
import fs2.concurrent.Queue
import fs2.{Pipe, Stream}
import monix.eval.Task

import scala.concurrent.duration.DurationInt

object ServerLogic extends StrictLogging {
  def bakePancakes(fryingPans: Int): Task[Either[String, Pipe[Task, PancakeIngredient, PancakeStatus]]] =
    // creating a queue is a Task; we bound the queue to 10 elements, not to overflow memory, and backpressure
    // the websocket if too many ingredients are arriving
    Queue.bounded[Task, PortionIngredients](10).map { fryingQueue =>
      Right { input: Stream[Task, PancakeIngredient] =>
        accumulate(input, fryingQueue).merge(fry(fryingPans, fryingQueue))
      }
    }

  // description a process which accumulates the incoming ingredients; when enough are ready, they are enqueued
  // for frying; once the frying process is free (see below), they will be dequeued and the actual frying will
  // commence
  private def accumulate(
      input: Stream[Task, PancakeIngredient],
      fryingQueue: Queue[Task, PortionIngredients]
  ): Stream[Task, IngredientReceived] =
    input
      .evalTap(ingredient => Task(logger.info(s"Received ingredient: $ingredient")))
      .evalMapAccumulate(Ingredients.Empty) { case (ingredients, ingredient) =>
        val newIngredients = ingredients + ingredient

        val (remainingIngredients, portionIngredients) = PortionIngredients.from(newIngredients)

        val enqueueFrying = portionIngredients match {
          case None => Task.now()
          case Some(pi) =>
            Task(logger.info(s"Frying pancakes: $pi; remaining ingredients: $remainingIngredients")) >> fryingQueue.enqueue1(pi)
        }

        enqueueFrying.map { _ => (remainingIngredients, IngredientReceived(ingredient)) }
      }
      .map(_._2)

  // description of a process which dequeues ingredients from the given `fryingQueue`, and fries them using
  // up to `fryingPans` in parallel
  private def fry(fryingPans: Int, fryingQueue: Queue[Task, PortionIngredients]): Stream[Task, PancakeReady.type] =
    fryingQueue.dequeue.flatMap { portionIngredients =>
      val singlePanPancakesCount = portionIngredients.pancakeCount / fryingPans
      val extraPancakesCount = portionIngredients.pancakeCount % fryingPans

      // assigning the number of pancakes to fry to each pan; distributing evenly as possible, and adding one extra
      // pancake to the remaining ones
      val pancakesPerFryingPan = (1 to fryingPans).map(i => singlePanPancakesCount + (if (i <= extraPancakesCount) 1 else 0))

      Stream(pancakesPerFryingPan: _*) // a stream of numbers
        .map(fryingPan) // a stream of stream descriptions
        .parJoinUnbounded // flattening the stream of streams by running them all in parallel
    }

  // a process simulating the behavior of a single frying pan: we assume that the ingredients are ready; frying
  // one pancake takes one second
  private def fryingPan(count: Int): Stream[Task, PancakeReady.type] = Stream.awakeEvery[Task](1.second).map(_ => PancakeReady).take(count)

  // data classes that are used by the streams

  private case class Ingredients(flourGrams: Int, milkLiters: Double, eggsCount: Int) {
    def +(i: PancakeIngredient): Ingredients = i match {
      case Flour(grams) => copy(flourGrams = flourGrams + grams)
      case Milk(liters) => copy(milkLiters = milkLiters + liters)
      case Eggs(count)  => copy(eggsCount = eggsCount + count)
    }
    def -(i: Ingredients): Ingredients = Ingredients(flourGrams - i.flourGrams, milkLiters - i.milkLiters, eggsCount - i.eggsCount)
    def *(n: Int): Ingredients = Ingredients(flourGrams * n, milkLiters * n, eggsCount * n)
  }

  private object Ingredients {
    val Empty: Ingredients = Ingredients(0, 0.0, 0)
  }

  private case class PortionIngredients(ingredients: Ingredients, pancakeCount: Int)

  private object PortionIngredients {
    val SinglePortion: Ingredients = Ingredients(250, 0.35, 2) // e.g. https://www.kwestiasmaku.com/kuchnia_polska/nalesniki/nalesniki.html
    val SinglePortionPancakeCount = 10

    /** @return If enough ingredients are accumulated, the remaining ingredients and the ingredients necessary for
      *         frying, along with the number of pancakes that can be fried with them
      */
    def from(ingredients: Ingredients): (Ingredients, Option[PortionIngredients]) = {
      val portions = Math.min(
        ingredients.flourGrams / SinglePortion.flourGrams,
        Math.min((ingredients.milkLiters / SinglePortion.milkLiters).toInt, ingredients.eggsCount / SinglePortion.eggsCount)
      )
      if (portions == 0) {
        (ingredients, None)
      } else {
        // ingredient math!
        val portionIngredients = SinglePortion * portions
        (ingredients - portionIngredients, Some(PortionIngredients(portionIngredients, SinglePortionPancakeCount * portions)))
      }
    }
  }
}
