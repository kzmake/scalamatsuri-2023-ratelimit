package com.github.kzmake.throttling

import scala.collection.concurrent.TrieMap

import com.github.kzmake.kvstore.KVStoreIOTypes._
import com.github.kzmake.throttling.ThrottlingIOTypes._
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._

object ThrottlingIOEffect extends ThrottlingIOEffect
trait ThrottlingIOEffect  extends ThrottlingIOOps

trait ThrottlingIOOps {
  implicit class ThrottlingIOOps[R, A](effects: Eff[R, A]) {
    def runThrottlingIO[U1, U2](
        implicit
        interpreter: ThrottlingIOInterpreter,
        m1: Member.Aux[ThrottlingIO, R, U1],
        m2: Member.Aux[StateKeyCost, U1, U2],
        mk: _kvstoreio[U1],
        me: _throwableEither[U1],
      ): Eff[U2, A] =
      interpreter
        .run(effects << ThrottlingIO.throttle)
        .evalStateU[TrieMap[String, Int], U2](TrieMap.empty)
  }
}
