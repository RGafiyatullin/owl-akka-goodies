package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.pipeline

trait Pipeline[AccT] {
  val key: PipelineKey
  val value: AccT
}
