package com.github.kzmake.kvstore

import cats.data.State
import org.atnos.eff._
import scala.collection.concurrent.TrieMap

object KVStoreIOTypes {
  type _kvstoreio[R] = KVStoreIO |= R

  type StateKeyValue[A]  = State[TrieMap[String, (Long, Long)], A]
  type _stateKeyValue[R] = StateKeyValue /= R

  type KVStoreIOStack = Fx.fx2[KVStoreIO, StateKeyValue]
}
