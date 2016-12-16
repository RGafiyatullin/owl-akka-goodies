package com.github.rgafiyatullin.owl_akka_goodies.terminator

import akka.actor.{ActorRef, ActorRefFactory, ActorSystem, Props}
import com.github.rgafiyatullin.owl_akka_goodies.actor_api.{ActorApi, ActorApiObject}

object TerminatorApi extends ActorApiObject[TerminatorApi] {
  final case class InitArgs(watchFor: ActorRef, systemToShut: ActorSystem)
  override val defaultActorName = "terminator"

  override def create(initArgs: InitArgs, name: String)(implicit arf: ActorRefFactory): TerminatorApi =
    TerminatorApi(arf.actorOf(Props(classOf[TerminatorActor], initArgs), name))

  object requests {
    final case class AddOnBeforeShutdown(f: () => Unit)
  }
}

final case class TerminatorApi(actor: ActorRef) extends ActorApi {
  def addOnBeforeShutdown(f: () => Unit): Unit =
    actor ! TerminatorApi.requests.AddOnBeforeShutdown(f)
}
