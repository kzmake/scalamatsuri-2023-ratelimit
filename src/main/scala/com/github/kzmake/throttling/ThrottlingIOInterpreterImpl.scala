package com.github.kzmake.throttling

import java.time.Instant

import scala.collection.concurrent.TrieMap

import cats.syntax.all._
import com.github.kzmake.error.TooManyRequestError
import com.github.kzmake.kvstore.KVStoreIO
import com.github.kzmake.kvstore.KVStoreIOTypes._
import com.github.kzmake.throttling.ThrottlingIO._
import com.github.kzmake.throttling.ThrottlingIOTypes._
import org.atnos.eff.Interpret.translate
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.either.errorTranslate
import org.atnos.eff.syntax.all._

class ThrottlingIOInterpreterImpl() extends ThrottlingIOInterpreter {
  def now[R]: Eff[R, Long]                      = Instant.now().toEpochMilli.pureEff[R] // !!
  def getBucket[R](key: String): Eff[R, Bucket] = Bucket.mock(key).pureEff[R]           // !!

  def validate[R: _kvstoreio: _throwableEither](key: String, cost: Long, ta: Long): Eff[R, Unit] = for {
    tat <- KVStoreIO.get(key).map(_.getOrElse(ta))
    b   <- getBucket(key)
    _   <- {
      // [ms]
      val `t_a`      = ta
      val T          = b.leak
      val tau        = b.size * T
      val `TAT_n`    = tat
      val `TAT_n+1`  = `TAT_n` + cost * T
      val retryAfter = `TAT_n+1` - `t_a` - (tau + T)
      if (`t_a` + (tau + T) - `TAT_n+1` > 0)
        right(())
      else
        left(
          TooManyRequestError(
            message = s"error: rate limit!!: retry after $retryAfter[ms] (bucket: $key)",
            retryAfter = (retryAfter / 1000.0).ceil.toLong,
          ),
        )
    }
  } yield ()
  def update[R: _kvstoreio](key: String, cost: Long, ta: Long): Eff[R, Unit]                     = for {
    tat <- KVStoreIO.get(key).map(_.getOrElse(ta))
    b   <- getBucket(key)
    _   <- {
      val `t_a`     = ta
      val T         = b.leak
      val tau       = b.size * T
      val `TAT_n`   = tat
      val `TAT_n+1` = `TAT_n` + cost * T
      val expireAt  = `TAT_n+1` - `t_a`
      KVStoreIO.setEx(key, `TAT_n+1`, expireAt)
    }
  } yield ()

  override def run[R, U, A](
      effects: Eff[R, A],
    )(implicit
      m: Member.Aux[ThrottlingIO, R, U],
      ms: _stateKeyCost[U],
      mk: _kvstoreio[U],
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
          x <- StateEffect.get[U, TrieMap[String, Int]].map(_.toSeq)
          n <- now[U]
          _ <- x.traverse { case (key, cost) => validate[U](key, cost, n) }
          _ <- x.traverse { case (key, cost) => update[U](key, cost, n) }

          // debug
          _ = x.map { case (key, cost) => println(s"  ts($n): ...$key -> removed $cost") }
          _ = println("")

        } yield ()
    }
  })
}
