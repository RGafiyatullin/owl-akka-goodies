package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.pipeline

import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin.PluginKey
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.wiring.Priority

object PipelineHandlers {
  final case class Handler(pluginKey: PluginKey, token: Any, priority: Priority = Priority.Normal)
}

final case class PipelineHandlers(key: PipelineKey, handlers: Seq[PipelineHandlers.Handler])
