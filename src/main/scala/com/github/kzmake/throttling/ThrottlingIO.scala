package com.github.kzmake.throttling

trait ThrottlingIO[A]

object ThrottlingIO extends ThrottlingIOCreation {
  case class Use(bucket: String, cost: Int) extends ThrottlingIO[Unit]
  case class Throttle()                     extends ThrottlingIO[Unit]
}
