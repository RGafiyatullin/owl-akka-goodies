package tests.plugin_system

import akka.actor.ActorSystem
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.event.{Event, EventHandlers, EventKey}
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.pipeline.{Pipeline, PipelineHandlers, PipelineKey}
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.actions.query.{Query, QueryKey}
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin.{Plugin, PluginKey}
import com.github.rgafiyatullin.owl_akka_goodies.plugin_support.plugin_system.PluginSystem
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ExecutionContext, Future}

object PluginSystemTest {
  implicit val actorSystem = ActorSystem("plugin-system-test")
  implicit val executionContext = actorSystem.dispatcher

  object events {
    case object e123 extends EventKey
    case class e123(n: Int = 3) extends Event {
      override val key: EventKey = e123
    }
    case object e12 extends EventKey
    case class e12(n: Int = 5) extends Event {
      override val key: EventKey = e12
    }
    case object e13 extends EventKey
    case class e13(n: Int = 7) extends Event {
      override val key: EventKey = e13
    }
    case object e23 extends EventKey
    case class e23(n: Int = 11) extends Event {
      override val key: EventKey = e23
    }
  }
  object pipelines {
    case object p1 extends PipelineKey
    final case class p1(value: Int = 0) extends Pipeline[Int] {
      override val key: PipelineKey = p1
    }

    case object p2 extends PipelineKey
    final case class p2(value: Int = 0) extends Pipeline[Int] {
      override val key: PipelineKey = p2
    }
  }
  object queries {
    case object q1 extends QueryKey
    case class q1(m: Int) extends Query[Int] {
      override val key: QueryKey = q1
    }
    case object q2 extends QueryKey
    case class q2(m: Int) extends Query[Int] {
      override val key: QueryKey = q2
    }
    case object q3 extends QueryKey
    case class q3(m: Int) extends Query[Int] {
      override val key: QueryKey = q3
    }
    case object qr extends QueryKey
    case class qr(other: PluginKey, m: Int) extends Query[Int] {
      override val key: QueryKey = qr
    }
    case object q extends QueryKey
    case class q(m: Int) extends Query[Int] {
      override val key: QueryKey = q
    }
  }
  object tokens {
    case object t1
    case object t2
    case object t3
  }

  object TestSystem {
    case object P1 extends PluginKey
    case class P1(counter: Int = 0) extends Plugin[TestSystem] {
      override val key: PluginKey = P1

      override def handleEvent: PartialFunction[(EventKey, Any), EventHandlerFunction] =
        Map(
          (events.e12, tokens.t1) -> {
            case (e: events.e12, s: TestSystem) =>
              Future.successful(copy(counter = counter + e.n), s)
          },
          (events.e13, tokens.t1) -> {
            case (e: events.e13, s: TestSystem) =>
              Future.successful(copy(counter = counter + e.n), s)
          },
          (events.e123, tokens.t1) -> {
            case (e: events.e123, s: TestSystem) =>
              Future.successful(copy(counter = counter + e.n), s)
          }
        )

      override def handleQuery(query: Any, system: TestSystem): Future[(Any, Self, TestSystem)] =
        query match {
          case queries.q1(m) => Future.successful(m * counter, copy(counter = -1), system)
          case queries.qr(other, m) =>
            for {
              (result, system) <- system.queryPlugin(other, queries.q(m))
            }
              yield (result, this, system)
          case queries.q(m) =>
            Future.successful(counter, copy(counter + m), system)
        }

      override def handlePipeline: PartialFunction[(PipelineKey, Any), PipelineHandlerFunction] =
        Map(
          (pipelines.p1, tokens.t1) -> {
            case (p: pipelines.p1, s: TestSystem) =>
              Future.successful(p.copy(p.value + 1), copy(counter = counter + p.value), s)
          },
          (pipelines.p1, tokens.t2) -> {
            case (p: pipelines.p1, s: TestSystem) =>
              Future.successful(p.copy(p.value + 2), copy(counter = counter + p.value), s)
          }
        )
    }

    case object P2 extends PluginKey
    case class P2(counter: Int = 0) extends Plugin[TestSystem] {
      override val key: PluginKey = P2

      override def handleEvent: PartialFunction[(EventKey, Any), EventHandlerFunction] =
        Map(
          (events.e12, tokens.t2) -> {
            case (e: events.e12, s: TestSystem) =>
              Future.successful(copy(counter = counter + e.n), s)
          },
          (events.e23, tokens.t2) -> {
            case (e: events.e23, s: TestSystem) =>
              Future.successful(copy(counter = counter + e.n), s)
          },
          (events.e123, tokens.t2) -> {
            case (e: events.e123, s: TestSystem) =>
              Future.successful(copy(counter = counter + e.n), s)
          }
        )

      override def handleQuery(query: Any, system: TestSystem): Future[(Any, Self, TestSystem)] =
        query match {
          case queries.q2(m) =>
            Future.successful(m * counter, copy(counter = -1), system)
          case queries.qr(other, m) =>
            for {
              (result, system) <- system.queryPlugin(other, queries.q(m))
            }
              yield (result, this, system)
          case queries.q(m) =>
            Future.successful(counter, copy(counter + m), system)
        }

      override def handlePipeline: PartialFunction[(PipelineKey, Any), PipelineHandlerFunction] =
        Map(
          (pipelines.p1, tokens.t1) -> {
            case (p: pipelines.p1, s: TestSystem) =>
              Future.successful(p.copy(p.value + 1), copy(counter = counter + p.value), s)
          },
          (pipelines.p1, tokens.t2) -> {
            case (p: pipelines.p1, s: TestSystem) =>
              Future.successful(p.copy(p.value + 2), copy(counter = counter + p.value), s)
          }
        )
    }

    case object P3 extends PluginKey
    case class P3(counter: Int = 0) extends Plugin[TestSystem] {
      override val key: PluginKey = P3

      override def handleEvent: PartialFunction[(EventKey, Any), EventHandlerFunction] =
        Map(
          (events.e23, tokens.t3) -> {
            case (e: events.e23, s: TestSystem) =>
              Future.successful(copy(counter = counter + e.n), s)
          },
          (events.e13, tokens.t3) -> {
            case (e: events.e13, s: TestSystem) =>
              Future.successful(copy(counter = counter + e.n), s)
          },
          (events.e123, tokens.t3) -> {
            case (e: events.e123, s: TestSystem) =>
              Future.successful(copy(counter = counter + e.n), s)
          }
        )

      override def handleQuery(query: Any, system: TestSystem): Future[(Any, Self, TestSystem)] =
        query match {
          case queries.q3(m) => Future.successful(m * counter, copy(counter = -1), system)
          case queries.qr(other, m) =>
            for {
              (result, system) <- system.queryPlugin(other, queries.q(m))
            }
              yield (result, this, system)
          case queries.q(m) =>
            Future.successful(counter, copy(counter + m), system)
        }

      override def handlePipeline: PartialFunction[(PipelineKey, Any), PipelineHandlerFunction] =
        Map(
          (pipelines.p1, tokens.t1) -> {
            case (p: pipelines.p1, s: TestSystem) =>
              Future.successful(p.copy(p.value + 1), copy(counter = counter + p.value), s)
          },
          (pipelines.p1, tokens.t2) -> {
            case (p: pipelines.p1, s: TestSystem) =>
              Future.successful(p.copy(p.value + 2), copy(counter = counter + p.value), s)
          }
        )
    }

    val plugins: Map[PluginKey, Plugin[TestSystem]] =
      Seq(
        P1(),
        P2(),
        P3()).map(p => p.key -> p).toMap

    val eventHandlers: Map[EventKey, EventHandlers] = Seq(
      EventHandlers(events.e123, Seq(
        EventHandlers.Handler(P1, tokens.t1),
        EventHandlers.Handler(P2, tokens.t2),
        EventHandlers.Handler(P3, tokens.t3)
      )),
      EventHandlers(events.e12, Seq(
        EventHandlers.Handler(P1, tokens.t1),
        EventHandlers.Handler(P2, tokens.t2)
      )),
      EventHandlers(events.e13, Seq(
        EventHandlers.Handler(P1, tokens.t1),
        EventHandlers.Handler(P3, tokens.t3)
      )),
      EventHandlers(events.e23, Seq(
        EventHandlers.Handler(P2, tokens.t2),
        EventHandlers.Handler(P3, tokens.t3)
      ))
    ).map(h => h.key -> h).toMap

    val pipelineHandlers: Map[PipelineKey, PipelineHandlers] = Seq(
      PipelineHandlers(pipelines.p1, Seq(
        PipelineHandlers.Handler(P1, tokens.t1),
        PipelineHandlers.Handler(P2, tokens.t1),
        PipelineHandlers.Handler(P1, tokens.t2),
        PipelineHandlers.Handler(P2, tokens.t3)
      ))
    ).map(h => h.key -> h).toMap

    def create(): TestSystem = TestSystem(executionContext, PluginSystem.Internals(plugins, eventHandlers, pipelineHandlers))
  }

  case class TestSystem(
                         executionContext: ExecutionContext,
                         internals: PluginSystem.Internals[TestSystem])
    extends PluginSystem[TestSystem]
  {
    //    override val metricsEntityName: String = "test-system"
    //    override val metricsCategory: String = "test-plugin-system"

    override def setInternals(i: PluginSystem.Internals[TestSystem]): TestSystem =
      copy(internals = i)
  }

}

class PluginSystemTest extends FlatSpec with Matchers with ScalaFutures {
  import PluginSystemTest._

  "PluginSystem" should "initialize" in {
    val s0 = TestSystem.create()
  }

  it should "dispatch events" in {
    val system = TestSystem.create()
    val futSystem =
      for {
      // P1(0), P2(0), P3(0)
        system <- system.runEvent(events.e123()) // +3 => P1(3), P2(3), P3(3)
        system <- system.runEvent(events.e12()) // +5 => P1(8), P2(8), P3(3)
        system <- system.runEvent(events.e13()) // +7 => P1(15), P2(8), P3(10)
        system <- system.runEvent(events.e23()) // +11 => P1(15), P2(19), P3(21)
      } yield system

    whenReady(futSystem) { sysComplete =>
      sysComplete.plugins(TestSystem.P1) should be (TestSystem.P1(15))
      sysComplete.plugins(TestSystem.P2) should be (TestSystem.P2(19))
      sysComplete.plugins(TestSystem.P3) should be (TestSystem.P3(21))
    }
  }

  it should "perform queries to plugins" in {
    val system = TestSystem.create()

    val results =
      for {
      // P1(0), P2(0), P3(0)
        system <- system.runEvent(events.e123()) // +3 => P1(3), P2(3), P3(3)
        system <- system.runEvent(events.e12()) // +5 => P1(8), P2(8), P3(3)
        system <- system.runEvent(events.e13()) // +7 => P1(15), P2(8), P3(10)
        system <- system.runEvent(events.e23()) // +11 => P1(15), P2(19), P3(21)
        (result1, system) <- system.queryPlugin(TestSystem.P1, queries.q1(2))
        (result2, system) <- system.queryPlugin(TestSystem.P2, queries.q2(3))
        (result3, system) <- system.queryPlugin(TestSystem.P3, queries.q3(4))
      }
        yield (result1, result2, result3, system)

    whenReady(results) {
      case (r1, r2, r3, sys) =>
        (r1, r2, r3) should be ((Some(30), Some(57), Some(84)))
        sys.plugins(TestSystem.P1) should be (TestSystem.P1(-1))
        sys.plugins(TestSystem.P2) should be (TestSystem.P2(-1))
        sys.plugins(TestSystem.P3) should be (TestSystem.P3(-1))
    }
  }

  it should "provide ability for plugins to invoke each other" in {
    val system = TestSystem.create()

    val results =
      for {
        (_, system) <- system.queryPlugin(TestSystem.P1, queries.qr(TestSystem.P2, 1))
        (_, system) <- system.queryPlugin(TestSystem.P2, queries.qr(TestSystem.P3, 2))
        (_, system) <- system.queryPlugin(TestSystem.P3, queries.qr(TestSystem.P1, 3))
      }
        yield system

    whenReady(results) { sys =>
      sys.plugins(TestSystem.P1) should be (TestSystem.P1(3))
      sys.plugins(TestSystem.P2) should be (TestSystem.P2(1))
      sys.plugins(TestSystem.P3) should be (TestSystem.P3(2))
    }
  }

  it should "run pipeline" in {
    val system = TestSystem.create()

    val results = system.runPipeline(pipelines.p1(1))
    /*
     [0, 0, 0]
     p(1)
      [1, 0, 0] -> p(2)
      [1, 2, 0] -> p(3)
      [4, 2, 0] -> p(5)
      [4, 2, 0] -> p(5)
     */
    whenReady(results) {
      case (acc, sys) =>
        acc should be (5)
        sys.plugins(TestSystem.P1) should be (TestSystem.P1(4))
        sys.plugins(TestSystem.P2) should be (TestSystem.P2(2))
        sys.plugins(TestSystem.P3) should be (TestSystem.P3(0))
    }
  }

  it should "run empty pipeline" in {
    val system = TestSystem.create()

    val results = system.runPipeline(pipelines.p2(1))

    whenReady(results) {
      case (acc, sys) =>
        acc should be (1)
        sys.plugins(TestSystem.P1) should be (TestSystem.P1(0))
        sys.plugins(TestSystem.P2) should be (TestSystem.P2(0))
        sys.plugins(TestSystem.P3) should be (TestSystem.P3(0))
    }
  }

}