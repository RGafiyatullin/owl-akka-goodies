package com.github.rgafiyatullin.owl_akka_goodies.terminator

import akka.actor.{ActorRef, ActorRefFactory, Props}
import com.github.rgafiyatullin.owl_akka_goodies.actor_api.{ActorApi, ActorApiObject}

object TerminatorApi extends ActorApiObject[TerminatorApi] {
  final case class InitArgs(watchFor: ActorRef)
  override val defaultActorName = "terminator"

  override def create(initArgs: InitArgs, name: String)(implicit arf: ActorRefFactory): TerminatorApi =
    TerminatorApi(arf.actorOf(Props(classOf[TerminatorActor], initArgs), name))
}

final case class TerminatorApi(actor: ActorRef) extends ActorApi
