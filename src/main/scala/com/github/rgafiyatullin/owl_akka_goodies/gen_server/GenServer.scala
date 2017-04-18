package com.github.rgafiyatullin.owl_akka_goodies.gen_server

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.github.rgafiyatullin.owl_akka_goodies.actor_future.{ActorFuture, ActorStdReceive}

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object GenServer {
  final case class Call(request: Any)

  def call(actorRef: ActorRef, request: Any)(implicit timeout: Timeout): Future[Any] =
    actorRef.ask(Call(request))

  final case class Cast(request: Any)
  def cast(actorRef: ActorRef, request: Any): Unit =
    actorRef ! Cast(request)

  final case class ReplyTo(actorRef: ActorRef) {
    def success(positive: Any): Unit = actorRef ! akka.actor.Status.Success(positive)
    def failure(negative: Throwable): Unit = actorRef ! akka.actor.Status.Failure(negative)
    def result(result: Try[Any]): Unit =
      result match {
        case Success(positive) => success(positive)
        case Failure(negative) => failure(negative)
      }
  }

  sealed trait UnexpectedRequest extends Exception
  final case class UnexpectedCall(request: Any) extends UnexpectedRequest
  final case class UnexpectedCast(request: Any) extends UnexpectedRequest

  object results {
    final case class ReplyOk[State](replyWith: Any, state: State)
    final case class ReplyError[State](reason: Throwable, state: State)
    final case class NoReply[State](state: State)
    final case class ShutdownOk[State](state: State)
    final case class ShutdownError[State](reason: Throwable, state: State)
    final case class Become(receive: Actor.Receive)
  }

  object call_results {
    sealed trait HandleCallResult[State]
    object HandleCallResult {
      import results._

      final case class WrapReplyOk[State](value: ReplyOk[State]) extends HandleCallResult[State]
      final case class WrapReplyError[State](value: ReplyError[State]) extends HandleCallResult[State]
      final case class WrapNoReply[State](value: NoReply[State]) extends HandleCallResult[State]
      final case class WrapShutdownOk[State](value: ShutdownOk[State]) extends HandleCallResult[State]
      final case class WrapShutdownError[State](value: ShutdownError[State]) extends HandleCallResult[State]
      final case class WrapBecome[State](value: Become) extends HandleCallResult[State]
    }
  }

  object cast_results {
    sealed trait HandleCastResult[State]
    object HandleCastResult {
      import results._

      final case class WrapNoReply[State](value: NoReply[State]) extends HandleCastResult[State]
      final case class WrapShutdownOk[State](value: ShutdownOk[State]) extends HandleCastResult[State]
      final case class WrapShutdownError[State](value: ShutdownError[State]) extends HandleCastResult[State]
      final case class WrapBecome[State](value: Become) extends HandleCastResult[State]
    }

  }

  object info_results {
    sealed trait HandleInfoResult[State]
    object HandleInfoResult {
      import results._

      final case class WrapNoReply[State](value: NoReply[State]) extends HandleInfoResult[State]
      final case class WrapShutdownOk[State](value: ShutdownOk[State]) extends HandleInfoResult[State]
      final case class WrapShutdownError[State](value: ShutdownError[State]) extends HandleInfoResult[State]
      final case class WrapBecome[State](value: Become) extends HandleInfoResult[State]
    }
  }


  object implicits {
    import scala.language.implicitConversions
    import results._
    import call_results._
    import cast_results._
    import info_results._

    implicit def handleCallResultFromReplyOk[State](from: ReplyOk[State]): HandleCallResult[State] = HandleCallResult.WrapReplyOk(from)
    implicit def handleCallResultFromReplyError[State](from: ReplyError[State]): HandleCallResult[State] = HandleCallResult.WrapReplyError(from)
    implicit def handleCallResultFromNoReply[State](from: NoReply[State]): HandleCallResult[State] = HandleCallResult.WrapNoReply(from)
    implicit def handleCallResultFromShutdownOk[State](from: ShutdownOk[State]): HandleCallResult[State] = HandleCallResult.WrapShutdownOk(from)
    implicit def handleCallResultFromShutdownError[State](from: ShutdownError[State]): HandleCallResult[State] = HandleCallResult.WrapShutdownError(from)
    implicit def handleCallResultFromBecome[State](from: Become): HandleCallResult[State] = HandleCallResult.WrapBecome(from)

    implicit def handleCastResultFromNoReply[State](from: NoReply[State]): HandleCastResult[State] = HandleCastResult.WrapNoReply(from)
    implicit def handleCastResultFromShutdownOk[State](from: ShutdownOk[State]): HandleCastResult[State] = HandleCastResult.WrapShutdownOk(from)
    implicit def handleCastResultFromShutdownError[State](from: ShutdownError[State]): HandleCastResult[State] = HandleCastResult.WrapShutdownError(from)
    implicit def handleCastResultFromBecome[State](from: Become): HandleCastResult[State] = HandleCastResult.WrapBecome(from)

    implicit def handleInfoResultFromNoReply[State](from: NoReply[State]): HandleInfoResult[State] = HandleInfoResult.WrapNoReply(from)
    implicit def handleInfoResultFromShutdownOk[State](from: ShutdownOk[State]): HandleInfoResult[State] = HandleInfoResult.WrapShutdownOk(from)
    implicit def handleInfoResultFromShutdownError[State](from: ShutdownError[State]): HandleInfoResult[State] = HandleInfoResult.WrapShutdownError(from)
    implicit def handleInfoResultFromBecome[State](from: Become): HandleInfoResult[State] = HandleInfoResult.WrapBecome(from)

    implicit class GenServerActorRef(actorRef: ActorRef) {

      def call(request: Any)(implicit timeout: Timeout): Future[Any] =
        GenServer.call(actorRef, request)

      def cast(request: Any): Unit =
        GenServer.cast(actorRef, request)

      object genServerDebug {

      }

    }
  }
}

trait GenServer[Args, State] extends Actor with ActorFuture with ActorStdReceive {
  implicit val stateClassTag: ClassTag[State]

  val args: Args
  import GenServer.call_results.HandleCallResult
  import GenServer.cast_results.HandleCastResult
  import GenServer.info_results.HandleInfoResult
  import GenServer.ReplyTo

  override def receive: Receive =
    future.handle(init(args)) {
      case Success(state) =>
        internals.whenReady(state)
    }

  def comeback(state: State): Receive = internals.whenReady(state)

  private object internals {
    def whenReady(state: State): Receive =
      receiveCall(state) orElse
        receiveCast(state) orElse
        receiveInfo(state) orElse
        stdReceive.discard

    def onError(reason: Throwable, state: State): Receive =
      future.handle(terminate(reason, state)) {
        case _ =>
          throw reason
      }


    def processHandleCallResult(replyTo: ReplyTo, handleCallResult: HandleCallResult[State]): Receive =
      handleCallResult match {
        case HandleCallResult.WrapNoReply(noReply) =>
          whenReady(noReply.state)

        case HandleCallResult.WrapReplyOk(replyOk) =>
          replyTo.success(replyOk.replyWith)
          whenReady(replyOk.state)

        case HandleCallResult.WrapReplyError(replyError) =>
          replyTo.failure(replyError.reason)
          whenReady(replyError.state)

        case HandleCallResult.WrapShutdownOk(_) =>
          context stop self
          stdReceive.discard

        case HandleCallResult.WrapShutdownError(shutdownError) =>
          onError(shutdownError.reason, shutdownError.state)

        case HandleCallResult.WrapBecome(become) =>
          become.receive
      }

    def processHandleCastResult(handleCastResult: HandleCastResult[State]): Receive =
      handleCastResult match {
        case HandleCastResult.WrapShutdownOk(_) =>
          context stop self
          stdReceive.discard

        case HandleCastResult.WrapShutdownError(shutdownError) =>
          onError(shutdownError.reason, shutdownError.state)

        case HandleCastResult.WrapNoReply(noReply) =>
          whenReady(noReply.state)

        case HandleCastResult.WrapBecome(become) =>
          become.receive
      }

    def processHandleInfoResult(handleInfoResult: HandleInfoResult[State]): Receive =
      handleInfoResult match {
        case HandleInfoResult.WrapShutdownOk(_) =>
          context stop self
          stdReceive.discard

        case HandleInfoResult.WrapShutdownError(shutdownError) =>
          onError(shutdownError.reason, shutdownError.state)

        case HandleInfoResult.WrapNoReply(noReply) =>
          whenReady(noReply.state)

        case HandleInfoResult.WrapBecome(become) =>
          become.receive
      }

    def receiveCall(state: State): Receive = {
      case GenServer.Call(request) =>
        val replyTo = ReplyTo(sender())
        val handler = handleCall(replyTo, state)
        val nextReceive =
          if (handler.isDefinedAt(request)) {
            future.handle(handler(request)) {
              case Success(handleCallResult) =>
                processHandleCallResult(replyTo, handleCallResult)

              case Failure(reason) =>
                replyTo.failure(reason)
                onError(reason, state)
            }
          } else {
            val reason = GenServer.UnexpectedCall(request)
            replyTo.failure(reason)
            log.warning("Unexpected call: {}", request)
            whenReady(state)
          }

        context become nextReceive
    }

    def receiveCast(state: State): Receive = {
      case GenServer.Cast(request) =>
        val handler = handleCast(state)
        val nextReceive =
          if (handler.isDefinedAt(request)) {
            future.handle(handler(request)) {
              case Success(handleCastResult) =>
                processHandleCastResult(handleCastResult)

              case Failure(reason) =>
                onError(reason, state)
            }
          } else {
            log.warning("Unexpected cast: {}", request)
            whenReady(state)
          }
        context become nextReceive
    }

    def receiveInfo(state: State): Receive =
      new PartialFunction[Any, Unit] {
        val handler = handleInfo(state)

        override def isDefinedAt(msg: Any) =
          handler.isDefinedAt(msg)

        override def apply(msg: Any) =
          context become future.handle(handler(msg)) {
            case Success(handleInfoResult) =>
              processHandleInfoResult(handleInfoResult)

            case Failure(reason) =>
              onError(reason, state)
          }
      }
  }


  def init(args: Args): Future[State]

  type HandleCall = PartialFunction[Any, Future[HandleCallResult[State]]]
  type HandleCast = PartialFunction[Any, Future[HandleCastResult[State]]]
  type HandleInfo = PartialFunction[Any, Future[HandleInfoResult[State]]]

  def handleCall(replyTo: ReplyTo, state: State): HandleCall = Map.empty
  def handleCast(state: State): HandleCast = Map.empty
  def handleInfo(state: State): HandleInfo = Map.empty

  def terminate(reason: Throwable, state: State): Future[Unit] = Future.successful(())

}
