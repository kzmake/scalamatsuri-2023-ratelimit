package com.github.kzmake.error

case class TooManyRequestError(message: String, retryAfter: Long) extends Throwable
