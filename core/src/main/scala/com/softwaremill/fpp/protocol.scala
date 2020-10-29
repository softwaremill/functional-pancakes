package com.softwaremill.fpp

sealed trait PancakeIngredient
case class Flour(grams: Int) extends PancakeIngredient
case class Milk(liters: Double) extends PancakeIngredient
case class Eggs(count: Int) extends PancakeIngredient

sealed trait PancakeStatus
case class IngredientReceived(ingredient: PancakeIngredient) extends PancakeStatus
case object PancakeReady extends PancakeStatus
