package com.github.kzmake.throttling

import cats.syntax.all._
import com.github.kzmake.throttling.ThrottlingIO._
import com.github.kzmake.throttling.ThrottlingIOTypes._
import org.atnos.eff.Interpret.translate
import org.atnos.eff._
import org.atnos.eff.state.stateMemberInLens
import org.atnos.eff.syntax.all._
import java.time.Instant
import scala.collection.concurrent.TrieMap

class ThrottlingIOInterpreterImpl(
    kvs: TrieMap[Key, Long] = TrieMap.empty[Key, Long],
  ) extends ThrottlingIOInterpreter {
  override def run[R, U, A](
      effects: Eff[R, A],
    )(implicit
      m: Member.Aux[ThrottlingIO, R, U],
      mkc: _throttlingKeyCost[U],
    ): Eff[U, A] = translate(effects)(new Translate[ThrottlingIO, U] {
    def apply[X](v: ThrottlingIO[X]): Eff[U, X] = v match {
      case Use(key, cost) =>
        for {
          keyCost <- StateEffect.get[U, TrieMap[Key, Cost]]
          _ = keyCost.update(key, keyCost.getOrElse(key, 0) + cost)
          _ <- StateEffect.put[U, TrieMap[Key, Cost]](keyCost)
        } yield ()

      case Throttle() =>
        for {
          keyCost <- StateEffect.get[U, TrieMap[Key, Cost]]
          now = Instant.now().toEpochMilli
          ta  = now
          _   = keyCost.toList.map {
            case (key, cost) =>
              val tat_n = kvs
                .get(key)                 // get tat_n
                .filter(tat => tat > now) // expired
                .getOrElse(ta)            // if n = 0: use ta
              val T   = 1000 // ms // quota.leak.toMillis
              val tau = 3 * T
              val tat_n1 = tat_n + cost * T

              if (ta + (tau + T) - tat_n1 > 0) {
                println(s"ok: ${ta + (tau + T) - tat_n1}")
                kvs += (key -> tat_n1)
                Right(())
              }
              else {
                println(s"ng: ${ta + (tau + T) - tat_n1}")
                Left(())
              }
          }
        } yield ()
    }
  })
}
