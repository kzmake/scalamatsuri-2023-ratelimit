package com.github.kzmake.eff

import java.time.Instant

import scala.collection.concurrent.TrieMap

import cats.data.State
import cats.syntax.all._
import com.github.kzmake.eff._
import com.github.kzmake.eff.all._
import com.github.kzmake.error.TooManyRequestError
import org.atnos.eff.Interpret.translate
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._

trait ThrottlingIOEffect  extends ThrottlingIOCreation with ThrottlingIOInterpretation
object ThrottlingIOEffect extends ThrottlingIOEffect

trait ThrottlingIOTypes {
  type _throttlingio[R] = ThrottlingIO |= R

  type StateKeyCost[A]  = State[TrieMap[String, Int], A]
  type _stateKeyCost[R] = StateKeyCost /= R

  type ThrottlingIOStack = Fx.append[Fx.fx3[ThrottlingIO, StateKeyCost, ThrowableEither], KVStoreStack]
}
object ThrottlingIOTypes extends ThrottlingIOTypes

trait ThrottlingIOCreation  extends ThrottlingIOTypes {
  def use[R: _throttlingio](bucket: String, cost: Int): Eff[R, Unit] = Eff.send[ThrottlingIO, R, Unit](Use(bucket, cost))
  def throttle[R: _throttlingio]: Eff[R, Unit]                       = Eff.send[ThrottlingIO, R, Unit](Throttle())
}
object ThrottlingIOCreation extends ThrottlingIOCreation

trait ThrottlingIOInterpretation  extends ThrottlingIOTypes {
  def now[R]: Eff[R, Long] = Instant.now().toEpochMilli.pureEff[R] // !!

  def getBucket[R](key: String): Eff[R, Bucket] = Bucket.mock(key).pureEff[R] // !!

  def validate[R: _kvstore: _throwableEither](key: String, quantity: Long, ta: Long): Eff[R, Unit] = for {
    tat <- KVStoreEffect.get[R, Long](key).map(_.getOrElse(ta))
    b   <- getBucket[R](key)
    _   <- {
      // [ms]
      val T          = b.leak
      val tau        = b.size * T
      val newTAT     = tat + quantity * T
      val allowAt    = newTAT - (tau + T)
      val retryAfter = if (allowAt - ta > 0) allowAt - ta else 0

      if (ta + (tau + T) - newTAT > 0) // NOTE: now > allowAt
        right[R, Throwable, Unit](())
      else
        left[R, Throwable, Unit](
          TooManyRequestError(
            message = s"error: rate limit!! retry after $retryAfter[ms] (bucket: $key)",
            retryAfter = (retryAfter / 1000.0).ceil.toLong, // NOTE: ms -> ceil(sec)
          ),
        )
    }
  } yield ()

  def update[R: _kvstore](key: String, quantity: Long, ta: Long): Eff[R, Unit] = for {
    tat <- KVStoreEffect.get[R, Long](key).map(_.getOrElse(ta))
    b   <- getBucket(key)
    _   <- {
      // [ms]
      val T        = b.leak
      val tau      = b.size * T
      val newTAT   = tat + quantity * T
      val expireAt = newTAT
      val ttl      = expireAt - ta

      KVStoreEffect.setEx[R, Long](key, ttl, newTAT)
    }
  } yield ()

  def run[R, U, A](
      effects: Eff[R, A],
    )(implicit
      m: Member.Aux[ThrottlingIO, R, U],
      ms: _stateKeyCost[U],
      mk: _kvstore[U],
      me: _throwableEither[U],
    ): Eff[U, A] = translate(effects)(new Translate[ThrottlingIO, U] {
    def apply[X](v: ThrottlingIO[X]): Eff[U, X] = v match {
      case Use(bucket, cost) =>
        for {
          k <- bucket.pureEff[U]
          v <- StateEffect.get[U, TrieMap[String, Int]].map(_.getOrElse(k, 0))
          _ <- StateEffect.modify[U, TrieMap[String, Int]](s => s += (k -> (v + cost)))
        } yield ()

      case Throttle() =>
        for {
          xs <- StateEffect.get[U, TrieMap[String, Int]].map(_.toSeq)
          n  <- now[U]
          _  <- xs.traverse { case (key, cost) => validate[U](key, cost, n) }
          _  <- xs.traverse { case (key, cost) => update[U](key, cost, n) }

          // debug
          //  _ = xs.map { case (key, cost) => println(s"  ts($n): ...$key -> removed $cost") }
          // _ = println("")

        } yield ()
    }
  })
}
object ThrottlingIOInterpretation extends ThrottlingIOInterpretation

sealed trait ThrottlingIO[A]
final case class Use(bucket: String, cost: Int) extends ThrottlingIO[Unit]
final case class Throttle()                     extends ThrottlingIO[Unit]
