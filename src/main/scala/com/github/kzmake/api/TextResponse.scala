package com.github.kzmake.echo

import com.github.kzmake.error.TooManyRequestError
import zio.http._
import zio.http.model._

trait TextResponse {
  def toResponse(v: Either[Throwable, Response]): Response = v match {
    case Right(res) =>
      res

    case Left(TooManyRequestError(message, retryAfter)) =>
      Response(
        status = Status.TooManyRequests,
        body = Body.fromString(message),
      ).withRetryAfter(retryAfter.toString)
    case Left(err)                                      =>
      Response(
        status = Status.InternalServerError,
        body = Body.fromString(err.getMessage),
      )
  }
}
