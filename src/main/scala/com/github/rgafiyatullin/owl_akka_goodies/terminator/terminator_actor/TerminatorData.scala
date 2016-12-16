package com.github.rgafiyatullin.owl_akka_goodies.terminator.terminator_actor

import scala.collection.immutable.Queue

final case class TerminatorData(beforeShutdown: Queue[() => Unit] = Queue.empty) {
  def appendOnBeforeShutdown(f: () => Unit): TerminatorData =
    copy(beforeShutdown = beforeShutdown.enqueue(f))
}
