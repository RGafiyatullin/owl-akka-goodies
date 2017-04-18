package tests.gen_server

import akka.actor.{ActorSystem, Props}
import com.github.rgafiyatullin.owl_akka_goodies.gen_server.GenServer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import scala.reflect.{ClassTag, classTag}

object GenServerTest {
  object GenServerImpl1 {
    final case class Args()
    final case class State(calls: Int = 0, casts: Int = 0, infos: Int = 0)
  }

  class GenServerImpl1(val args: GenServerImpl1.Args)
    extends GenServer[GenServerImpl1.Args, GenServerImpl1.State]
  {
    val stateClassTag: ClassTag[GenServerImpl1.State] = classTag[GenServerImpl1.State]
    import GenServer.results._
    import GenServer.implicits._

    override def init(args: GenServerImpl1.Args) = Future.successful(GenServerImpl1.State())

    override def handleCall(replyTo: GenServer.ReplyTo, state: GenServerImpl1.State) = {
      case i: Int =>
        Future.successful(ReplyOk((), state.copy(calls = state.calls + i)))
    }

    override def handleCast(state: GenServerImpl1.State) = {
      case "dump_state" =>
        println("state: " + state)
        Future.successful(NoReply(state))
      case i: Int =>
        Future.successful(NoReply(state.copy(casts = state.casts + i)))
    }

    override def handleInfo(state: GenServerImpl1.State) = {
      case i: Int =>
        Future.successful(NoReply(state.copy(infos = state.infos + i)))
    }
  }
}

class GenServerTest extends FlatSpec with Matchers with ScalaFutures {
  import akka.util.Timeout
  import scala.concurrent.duration._
  implicit val timeout = Timeout(5.seconds)
  val actorSystem = ActorSystem("test-gen-server")

  "GenServer" should "initialize" in {
    val gs1 = actorSystem.actorOf(Props(classOf[GenServerTest.GenServerImpl1], GenServerTest.GenServerImpl1.Args()))
    GenServer.call(gs1, 1).mapTo[Unit]
    GenServer.cast(gs1, 2)
    gs1 ! 3

    GenServer.cast(gs1, "dump_state")

    whenReady(GenServer.call(gs1, 1).mapTo[Unit])(identity)
    whenReady(actorSystem.terminate())(identity)
    ()
  }
}
