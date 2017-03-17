package com.github.rgafiyatullin.owl_akka_goodies.util

import scala.concurrent.{ExecutionContext, Future}

object OptionFuture {
  implicit class OptionFutureSwap[T](optionFuture: Option[Future[T]]) {
    def toFutureOption(implicit ec: ExecutionContext): Future[Option[T]] =
      optionFuture match {
        case None => Future.successful(None)
        case Some(future) => future.map(Some(_))
      }
  }
}
