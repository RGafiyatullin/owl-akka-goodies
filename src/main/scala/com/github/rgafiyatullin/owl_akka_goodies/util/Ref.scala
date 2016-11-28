package com.github.rgafiyatullin.owl_akka_goodies.util

import java.util.UUID

object Ref {
  def create(): Ref = Ref(UUID.randomUUID())
}

final case class Ref(id: UUID)
