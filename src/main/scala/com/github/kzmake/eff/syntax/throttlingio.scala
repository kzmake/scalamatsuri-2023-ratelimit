package com.github.kzmake.eff.syntax

import scala.collection.concurrent.TrieMap

import com.github.kzmake.eff._
import com.github.kzmake.eff.all._
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._

trait throttlingio {
  implicit class ThrottlingIOEffectOps[R, A](e: Eff[R, A]) {
    def runThrottlingIO[U1, U2](
        implicit
        m1: Member.Aux[ThrottlingIO, R, U1],
        m2: Member.Aux[StateKeyCost, U1, U2],
        mk: _kvstore[U1],
        me: _throwableEither[U1],
      ): Eff[U2, A] =
      ThrottlingIOInterpretation
        .run(e << ThrottlingIOEffect.throttle)
        .evalStateU[TrieMap[String, Int], U2](TrieMap.empty)
  }
}
object throttlingio extends throttlingio
