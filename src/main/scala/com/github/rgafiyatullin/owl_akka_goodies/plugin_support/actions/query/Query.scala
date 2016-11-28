package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.query

trait Query[T] {
  val key: QueryKey

  def castResultType(result: Any): T = result.asInstanceOf[T]
}
