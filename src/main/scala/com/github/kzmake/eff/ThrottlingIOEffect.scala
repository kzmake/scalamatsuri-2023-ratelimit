package com.github.kzmake.eff

import cats.data.State
import cats.syntax.all._
import com.github.kzmake.eff._
import com.github.kzmake.eff.all._
import com.github.kzmake.error.TooManyRequestError

import scala.collection.concurrent.TrieMap
import org.atnos.eff._
import org.atnos.eff.Interpret.translate
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._

import java.time.Instant

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

  def validate[R: _kvstore: _throwableEither](key: String, cost: Long, ta: Long): Eff[R, Unit] = for {
    tat <- KVStoreEffect.get[R](key).map(_.getOrElse(ta))
    b   <- getBucket[R](key)
    _   <- {
      // [ms]
      val `t_a`      = ta
      val T          = b.leak
      val tau        = b.size * T
      val `TAT_n`    = tat
      val `TAT_n+1`  = `TAT_n` + cost * T
      val retryAfter = `TAT_n+1` - `t_a` - (tau + T)

      if (`t_a` + (tau + T) - `TAT_n+1` > 0)
        right[R, Throwable, Unit](())
      else
        left[R, Throwable, Unit](
          TooManyRequestError(
            message = s"error: rate limit!!: retry after $retryAfter[ms] (bucket: $key)",
            retryAfter = (retryAfter / 1000.0).ceil.toLong,
          ),
        )
    }
  } yield ()

  def update[R: _kvstore](key: String, cost: Long, ta: Long): Eff[R, Unit] = for {
    tat <- KVStoreEffect.get(key).map(_.getOrElse(ta))
    b   <- getBucket(key)
    _   <- {
      val `t_a`     = ta
      val T         = b.leak
      val tau       = b.size * T
      val `TAT_n`   = tat
      val `TAT_n+1` = `TAT_n` + cost * T
      val expireAt  = `TAT_n+1` - `t_a`
      KVStoreEffect.setEx(key, `TAT_n+1`, expireAt)
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
          //  _ = x.map { case (key, cost) => println(s"  ts($n): ...$key -> removed $cost") }
          // _ = println("")

        } yield ()
    }
  })
}
object ThrottlingIOInterpretation extends ThrottlingIOInterpretation

sealed trait ThrottlingIO[A]
final case class Use(bucket: String, cost: Int) extends ThrottlingIO[Unit]
final case class Throttle()                     extends ThrottlingIO[Unit]