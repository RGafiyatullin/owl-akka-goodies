package com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actor_goodies

import akka.pattern.ask
import akka.util.Timeout
import com.github.rgafiyatullin.owl_akka_goodies.actor_api.ActorApi
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.event.Event
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.query.Query
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin.PluginKey

import scala.concurrent.Future

object PluginSystemActorApi {
  object requests {
    final case class FireEvent(event: Event)
    final case class QueryPlugin[T](key: PluginKey, query: Query[T])
  }
}

trait PluginSystemActorApi extends ActorApi {
  def fireEvent
    (event: Event)
    (implicit callTimeout: Timeout)
  : Future[Unit] =
    actor
      .ask(PluginSystemActorApi.requests.FireEvent(event))
      .mapTo[Unit]

  def queryPlugin[T]
    (key: PluginKey, query: Query[T])
    (implicit callTimeout: Timeout)
  : Future[Option[T]] =
    actor
      .ask(PluginSystemActorApi.requests.QueryPlugin(key, query))
      .mapTo[Option[T]]
}
