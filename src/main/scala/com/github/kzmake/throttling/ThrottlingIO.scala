package com.github.kzmake.throttling

import com.github.kzmake.throttling.ThrottlingIOTypes._

trait ThrottlingIO[A]

object ThrottlingIO extends ThrottlingIOCreation {
  case class Use(bucket: Key, request: Cost) extends ThrottlingIO[Unit]
  case class Throttle()                      extends ThrottlingIO[Unit]
}
