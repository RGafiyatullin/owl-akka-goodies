package com.github.rgafiyatullin.owl_akka_goodies.actor_future

import akka.actor.{Actor, ActorLogging}

object ActorStdReceive {
  final case class UnexpectedMessageReceived(message: Any) extends Throwable
}

trait ActorStdReceive extends Actor with ActorLogging {
  object stdReceive {
    def discard: Receive = {
      case unexpected =>
        log.warning("Discarding: {}", unexpected)
    }

    def empty: Receive = {
      case unexpected =>
        log.warning("Received unexpected: {}", unexpected)
        throw ActorStdReceive.UnexpectedMessageReceived(unexpected)
    }
  }
}
