package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.event

import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin.PluginKey

object EventHandlers {
  final case class Handler(pluginKey: PluginKey, token: Any)
}

final case class EventHandlers(key: EventKey, handlers: Seq[EventHandlers.Handler])
