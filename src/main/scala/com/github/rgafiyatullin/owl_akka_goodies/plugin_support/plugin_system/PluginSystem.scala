package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin_system

import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.event.{Event, EventHandlers, EventKey}
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.pipeline.{Pipeline, PipelineHandlers, PipelineKey}
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.query.Query
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin.{Plugin, PluginKey}

import scala.collection.immutable.Queue
import scala.concurrent.{ExecutionContext, Future}

object PluginSystem {
  import scala.language.existentials

  sealed trait Pending
  final case class PendingEvent(event: Event) extends Pending
  final case class PendingQuery(pluginKey: PluginKey, query: Query[_]) extends Pending

  final case class Internals[System <: PluginSystem[System]](
    plugins: Map[PluginKey, Plugin[System]],
    allEventHandlers: Map[EventKey, EventHandlers],
    allPipelineHandlers: Map[PipelineKey, PipelineHandlers],
    disablePendings: Boolean = true,
    pendings: Queue[Pending] = Queue.empty)
}

trait PluginSystem[Self <: PluginSystem[Self]] {
  implicit val executionContext: ExecutionContext

  private val pluginSystem: Self = this.asInstanceOf[Self]

  def internals: PluginSystem.Internals[Self]
  def setInternals(i: PluginSystem.Internals[Self]): Self

  def plugins: Map[PluginKey, Plugin[Self]] =
    internals.plugins

  def takePlugin(key: PluginKey): (Plugin[Self], Self) =
    (internals.plugins(key),
      setInternals(internals.copy(plugins = internals.plugins - key)))

  def setPlugin(plugin: Plugin[Self]): Self =
    setInternals(internals.copy(plugins = plugins + (plugin.key -> plugin)))

  def enqueueEvent(event: Event): Self =
    setInternals(
      internals.copy(
        pendings = internals.pendings.enqueue(PluginSystem.PendingEvent(event))))

  def enqueueQuery(pluginKey: PluginKey, query: Query[_]): Self =
    setInternals(
      internals.copy(
        pendings = internals.pendings.enqueue(PluginSystem.PendingQuery(pluginKey, query))))

  def allEventHandlers: Map[EventKey, EventHandlers] =
    internals.allEventHandlers

  def allPipelineHandlers: Map[PipelineKey, PipelineHandlers] =
    internals.allPipelineHandlers


  def enableRunPendings: Self = setInternals(internals.copy(disablePendings = false))
  def disableRunPendings: Self = setInternals(internals.copy(disablePendings = true))

  def takeEnqueuedPending(): Option[(PluginSystem.Pending, Self)] =
    for {
      pending <- internals.pendings.headOption
    }
      yield (pending, setInternals(internals.copy(pendings = internals.pendings.drop(1))))

  def runPendings(): Future[Self] =
    if (internals.disablePendings) Future.successful(pluginSystem)
    else takeEnqueuedPending()
      .fold(Future.successful(pluginSystem)) {
        case (PluginSystem.PendingEvent(event), system) =>
          for {
            system <- system.disableRunPendings.runEvent(event)
            system <- system.enableRunPendings.runPendings()
          }
            yield system

        case (PluginSystem.PendingQuery(pk, query), system) =>
          for {
            (_, system) <- system.disableRunPendings.queryPlugin(pk, query)
            system <- system.enableRunPendings.runPendings()
          }
            yield system
      }

  def initializePlugins(): Future[Self] =
    disableRunPendings
      .plugins
      .keys
      .foldLeft(Future.successful(pluginSystem)) {
        case (futPS, pk) =>
          for {
            ps1 <- futPS
            (p, ps2) = ps1.takePlugin(pk)
            (p, ps3) <- p.init(ps2)
            ps4 = ps3.setPlugin(p)
          }
            yield ps4
      }
      .map(_.enableRunPendings)
      .flatMap(_.runPendings())

  def runEvent(event: Event): Future[Self] =
    allEventHandlers.get(event.key) match {
      case None => Future.successful(pluginSystem)
      case Some(handlers) =>
        handlers
          .handlers
          .foldLeft(Future.successful(pluginSystem)) {
            case (futSystem, handler) =>
              for {
                system <- futSystem
                system <- processEventWithHandler(event, handler, system)
              }
                yield system
          }.flatMap(_.runPendings())
}


  def runPipeline[AccT]
    (pipeline: Pipeline[AccT])
  : Future[(AccT, Self)] =
    allPipelineHandlers.get(pipeline.key) match {
      case None => Future.successful(pipeline.value, pluginSystem)
      case Some(handlers) =>
        handlers.handlers
          .foldLeft(Future.successful(pipeline, pluginSystem)) {
            case (futAccAndSystem, handler) =>
              for {
                (pipeline, system) <- futAccAndSystem
                (pipeline, system) <- processPipelineWithHandler(pipeline, handler, system)
                system <- system.runPendings()
              }
                yield (pipeline, system)
          }.map {
          case (pipelineOut, systemOut) =>
            (pipelineOut.value, systemOut)
        }
    }


  def queryPlugin[RetT]
    (key: PluginKey, query: Query[RetT])
  : Future[(Option[RetT], Self)] =
    plugins.get(key) match {
      case None =>
        Future.successful(None, pluginSystem)

      case Some(plugin) =>
        for {
          (result, plugin, system) <- plugin.handleQuery(query, pluginSystem)
          system <- system.setPlugin(plugin).runPendings()
        }
          yield (Some(query.castResultType(result)), system)
    }

  def processEventWithHandler
    (event: Event, handler: EventHandlers.Handler, system: PluginSystem[Self])
  : Future[Self] = {
    val (plugin, system1) = system.takePlugin(handler.pluginKey)
    val maybeHandled = plugin.handleEvent.lift((event.key, handler.token)).map(_(event, system1))

    for {
      (plugin, system2) <- maybeHandled.getOrElse(Future.successful(plugin, system1))
    }
      yield system2.setPlugin(plugin)
  }

  def processPipelineWithHandler[AccT]
    (pipeline: Pipeline[AccT], handler: PipelineHandlers.Handler, system: Self)
  : Future[(Pipeline[AccT], Self)] = {
    val (plugin, system1) = system.takePlugin(handler.pluginKey)
    val maybeHandled = plugin.handlePipeline.lift((pipeline.key, handler.token)).map(_(pipeline, system1))

    for {
      (pipeline, plugin, system2) <- maybeHandled.getOrElse(Future.successful(pipeline, plugin, system1))
    }
      yield (pipeline.asInstanceOf[Pipeline[AccT]], system2.setPlugin(plugin))
  }

}
