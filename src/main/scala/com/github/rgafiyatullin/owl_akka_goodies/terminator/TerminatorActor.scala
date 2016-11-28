package com.github.rgafiyatullin.owl_akka_goodies.terminator

import akka.actor.{Actor, ActorLogging, Terminated}

class TerminatorActor(initArgs: TerminatorApi.InitArgs) extends Actor with ActorLogging {
  log.info("Start watching for {}", initArgs.watchFor)
  context watch initArgs.watchFor

  override def receive: Receive = {
    case Terminated(initArgs.watchFor) =>
      log.warning("{} has terminated. Shutting {} down", initArgs.watchFor, context.system)
      context.system.terminate()
  }
}
