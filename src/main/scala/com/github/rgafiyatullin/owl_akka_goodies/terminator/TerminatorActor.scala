package com.github.rgafiyatullin.owl_akka_goodies.terminator

import akka.actor.{Actor, ActorLogging, Terminated}
import com.github.rgafiyatullin.owl_akka_goodies.actor_future.{ActorFuture, ActorStdReceive}
import com.github.rgafiyatullin.owl_akka_goodies.terminator.terminator_actor.TerminatorData

class TerminatorActor(args: TerminatorApi.InitArgs) extends Actor with ActorStdReceive with ActorLogging {
  // force initialization of ActorLogging._log
  private val _ = log

  def init(): TerminatorData = TerminatorData()

  def whenReady(data: TerminatorData): Receive = {
    case TerminatorApi.requests.AddOnBeforeShutdown(f) =>
      context become whenReady(data.appendOnBeforeShutdown(f))

    case Terminated(args.watchFor) =>
      log.info("TopSup is dead. About to execution PreShutdown actions...")
      data.beforeShutdown.foreach(beforeShutdownAction => beforeShutdownAction())
      log.info("TopSup is dead. Shutting the ActorSystem[{}] down...", args.systemToShut.name)
      args.systemToShut.terminate()
      context become stdReceive.discard
  }

  override def receive: Receive = whenReady(init())

  context watch args.watchFor
}
