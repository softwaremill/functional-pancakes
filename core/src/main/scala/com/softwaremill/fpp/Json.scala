package com.softwaremill.fpp

import io.circe.Codec
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto.deriveConfiguredCodec

// this codecs contained in this object must be imported when we want to serialise a ingredient/status to json,
// or parse from json
object Json extends PancakeIngredientCodecs with PancakeStatusCodecs

// ingredient codecs are needed to derive codec for IngredientReceived
trait PancakeStatusCodecs extends PancakeIngredientCodecs {
  // the discriminator field will contain the name of the class in snake-case
  private implicit val config: Configuration = Configuration.default
    .withDiscriminator("status")
    .withSnakeCaseConstructorNames

  implicit val pancakeStatusCodec: Codec[PancakeStatus] = deriveConfiguredCodec[PancakeStatus]
}

trait PancakeIngredientCodecs {
  // for ingredients, we are using a different discriminator field, and we need another configuration
  private implicit val config: Configuration = Configuration.default
    .withDiscriminator("ingredient")
    .withSnakeCaseConstructorNames

  implicit val pancakeIngredientCodec: Codec[PancakeIngredient] = deriveConfiguredCodec[PancakeIngredient]
}
