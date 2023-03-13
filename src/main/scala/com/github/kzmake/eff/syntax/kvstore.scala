package com.github.kzmake.eff.syntax

import scala.collection.concurrent.TrieMap

import com.github.kzmake.eff._
import com.github.kzmake.eff.all._
import org.atnos.eff._
import org.atnos.eff.syntax.all._

trait kvstore {
  implicit class KVStoreEffectOps[R, A](e: Eff[R, A]) {
    def runKVStore[U1, U2](
        store: TrieMap[String, (Long, Any)],
      )(implicit
        m1: Member.Aux[KVStore, R, U1],
        m2: Member.Aux[StateKeyValue, U1, U2],
      ): Eff[U2, A] =
      KVStoreInterpretation
        .run(e)
        .evalStateU[TrieMap[String, (Long, Any)], U2](store)
  }
}
object kvstore extends kvstore
