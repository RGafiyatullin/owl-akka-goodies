package com.github.rgafiyatullin.owl_akka_goodies.actor_future

import akka.actor.{Actor, Stash}
import com.github.rgafiyatullin.owl_akka_goodies.util.Ref

import scala.concurrent.Future
import scala.reflect.{ClassTag, classTag}
import scala.util.{Failure, Success, Try}

object ActorFuture {
  final case class UnexpectedSuccess[T](value: T) extends Throwable

  final case class UnexpectedFailure(reason: Throwable) extends Throwable {
    override def getCause = reason
  }

}

trait ActorFuture extends Actor with Stash {
  private implicit lazy val executionContext = context.dispatcher

  object future {
    def handle[T: ClassTag]
      (futureValue: Future[T])
      (handleWith: PartialFunction[Try[T], Receive])
    : Receive = {
      val ref = Ref.create()
      val me = self

      futureValue.onComplete(me ! (ref, _))

      PartialFunction[Any, Unit] {
        case (`ref`, success @ Success(value)) if classTag[T].runtimeClass.isInstance(value) =>
          unstashAll()
          val successTyped = success.asInstanceOf[Try[T]]
          if (handleWith.isDefinedAt(successTyped))
            handleWith(successTyped)
          else
            throw ActorFuture.UnexpectedSuccess(value)

        case (`ref`, failure @ Failure(reason)) =>
          unstashAll()
          val failureTyped = failure.asInstanceOf[Try[T]]
          if (handleWith.isDefinedAt(failureTyped))
            handleWith(failureTyped)
          else
            throw ActorFuture.UnexpectedFailure(reason)

        case _ =>
          stash()
      }
    }
  }
}
