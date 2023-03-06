package com.github.kzmake.throttling

import com.github.kzmake.throttling.ThrottlingIO.Throttle
import com.github.kzmake.throttling.ThrottlingIOTypes._
import org.atnos.eff.syntax.all.StateEffectOps
import org.atnos.eff._

import scala.collection.concurrent.TrieMap

object ThrottlingIOEffect extends ThrottlingIOEffect
trait ThrottlingIOEffect  extends ThrottlingIOOps

trait ThrottlingIOOps {
  implicit class ThrottlingIOOps[R, A](effects: Eff[R, A]) {
    def runThrottlingIO[U1, U2](
        implicit
        interpreter: ThrottlingIOInterpreter,
        m1: Member.Aux[ThrottlingIO, R, U1],
        m2: Member.Aux[ThrottlingKeyCost, U1, U2],
        mkc: _throttlingKeyCost[U1],
      ): Eff[U2, A] =
      interpreter
        .run(effects << ThrottlingIO.throttle)
        .evalStateU[TrieMap[Key, Cost], U2](TrieMap.empty)
  }
}
