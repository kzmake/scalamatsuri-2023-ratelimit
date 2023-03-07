package com.github.kzmake.throttling

import scala.collection.concurrent.TrieMap

import cats.data.State
import com.github.kzmake.kvstore.KVStoreIOTypes.KVStoreIOStack
import org.atnos.eff._
import org.atnos.eff.all._

object ThrottlingIOTypes {
  type _throttlingio[R] = ThrottlingIO |= R

  type StateKeyCost[A]  = State[TrieMap[String, Int], A]
  type _stateKeyCost[R] = StateKeyCost /= R

  type ThrottlingIOStack = Fx.append[Fx.fx3[ThrottlingIO, StateKeyCost, ThrowableEither], KVStoreIOStack]
}
