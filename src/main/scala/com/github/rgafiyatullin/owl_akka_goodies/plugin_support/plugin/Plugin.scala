package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin

import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.event.{Event, EventKey}
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.pipeline.{Pipeline, PipelineKey}
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin_system.PluginSystem

import scala.concurrent.Future

trait Plugin[System <: PluginSystem[System]] {
  type Self = Plugin[System]
  type EventHandlerFunction = (Event, System) => Future[(Self, System)]
  type PipelineHandlerFunction = (Pipeline[_], System) => Future[(Pipeline[_], Self, System)]

  val key: PluginKey

  def init(system: System): Future[(Self, System)] = Future.successful(this, system)

  def handleEvent: PartialFunction[(EventKey, Any), EventHandlerFunction] = Map.empty
  def handlePipeline: PartialFunction[(PipelineKey, Any), PipelineHandlerFunction] = Map.empty

  def handleQuery(query: Any, system: System): Future[(Any, Self, System)] =
    Future.successful(None, this, system)
}
