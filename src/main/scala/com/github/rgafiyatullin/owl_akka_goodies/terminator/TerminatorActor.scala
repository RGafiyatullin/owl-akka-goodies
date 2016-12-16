package com.github.rgafiyatullin.owl_akka_goodies.terminator

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Terminated}
import com.github.rgafiyatullin.owl_akka_goodies.terminator.terminator_actor.TerminatorData

class TerminatorActor(topSup: ActorRef, actorSystem: ActorSystem) extends Actor with ActorLogging {
  // force initialization of ActorLogging._log
  private val _ = log

  def init(): TerminatorData = TerminatorData()

  def whenReady(data: TerminatorData): Receive = {
    case TerminatorApi.requests.AddOnBeforeShutdown(f) =>
      context become whenReady(data.appendOnBeforeShutdown(f))

    case Terminated(`topSup`) =>
      log.info("TopSup is dead. About to execution PreShutdown actions...")
      data.beforeShutdown.foreach(beforeShutdownAction => beforeShutdownAction())
      log.info("TopSup is dead. Shutting the ActorSystem[{}] down...", actorSystem.name)
      actorSystem.terminate()
  }

  override def receive: Receive = whenReady(init())

  context watch topSup
}
