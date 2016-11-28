package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.pipeline

import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin.PluginKey

object PipelineHandlers {
  final case class Handler(pluginKey: PluginKey, token: Any)
}

final case class PipelineHandlers(key: PipelineKey, handlers: Seq[PipelineHandlers.Handler])
