package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actor_goodies

import akka.actor.Actor
import akka.actor.Status.{Failure => AkkaFailure, Success => AkkaSuccess}
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin_system.PluginSystem

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class PluginSystemAccessBehaviour
  [ActorImpl <: Actor, System <: PluginSystem[System]]
  (actor: ActorImpl, system: System)
  (thenBecome: Function[Future[System], Unit])
  extends Actor.Receive
{
  override def isDefinedAt(msg: Any): Boolean =
    msg match {
      case PluginSystemActorApi.requests.FireEvent(_) =>
        true

      case PluginSystemActorApi.requests.QueryPlugin(_, _) =>
        true

      case _ =>
        false
    }

  override def apply(msg: Any): Unit =
    msg match {
      case PluginSystemActorApi.requests.FireEvent(event) =>
        implicit val ec = system.executionContext
        val replyTo = actor.sender()
        val futSystem = system.runEvent(event)
        futSystem onComplete {
          case Success(_) => replyTo ! AkkaSuccess(())
          case Failure(reason) => replyTo ! AkkaFailure(reason)
        }
        thenBecome(futSystem)

      case PluginSystemActorApi.requests.QueryPlugin(pluginKey, query) =>
        implicit val ec = system.executionContext
        val replyTo = actor.sender()
        val futResultAndSystem = system.queryPlugin(pluginKey, query)
        futResultAndSystem onComplete {
          case Success((result, _)) => replyTo ! AkkaSuccess(result)
          case Failure(reason) => replyTo ! AkkaFailure(reason)
        }
        thenBecome(futResultAndSystem.map(_._2))
    }

}
