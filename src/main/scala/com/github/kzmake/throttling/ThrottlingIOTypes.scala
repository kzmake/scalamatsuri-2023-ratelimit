package com.github.kzmake.throttling

import cats.data.State
import org.atnos.eff._

import scala.collection.concurrent.TrieMap

object ThrottlingIOTypes {
  type _throttlingio[R] = ThrottlingIO |= R

  type Key     = String
  type Cost    = Int

  type ThrottlingKeyCost[A]  = State[TrieMap[Key, Cost], A]
  type _throttlingKeyCost[R] = ThrottlingKeyCost /= R

  type ThrottlingIOStack = Fx.fx2[ThrottlingIO, ThrottlingKeyCost]
}
