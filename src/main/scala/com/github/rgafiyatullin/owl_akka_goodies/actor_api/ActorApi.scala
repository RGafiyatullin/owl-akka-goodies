package com.github.rgafiyatullin.owl_akka_goodies.actor_api

import akka.actor.{ActorRef, ActorRefFactory}

trait ActorApiObject[Api <: ActorApi] {
  type InitArgs
  val defaultActorName: String

  def create(initArgs: InitArgs, name: String = defaultActorName)(implicit arf: ActorRefFactory): Api
}

trait ActorApi {
  val actor: ActorRef
}
