package com.github.kzmake.kvstore

import com.github.kzmake.kvstore.KVStoreIOTypes._
import org.atnos.eff._
import org.atnos.eff.all._throwableEither
import org.atnos.eff.syntax.all._

import scala.collection.concurrent.TrieMap

object KVStoreIOEffect extends KVStoreIOEffect
trait KVStoreIOEffect  extends KVStoreIOOps

trait KVStoreIOOps {
  implicit class KVStoreIOOps[R, A](effects: Eff[R, A]) {
    def runKVStoreIO[U1, U2](store: TrieMap[String, (Long, Long)])(
        implicit
        interpreter: KVStoreIOInterpreter,
        m1: Member.Aux[KVStoreIO, R, U1],
        m2: Member.Aux[StateKeyValue, U1, U2],
        ms: _stateKeyValue[U1],
      ): Eff[U2, A] =
      interpreter
        .run(effects)
        .evalStateU[TrieMap[String, (Long, Long)], U2](store)
  }
}
