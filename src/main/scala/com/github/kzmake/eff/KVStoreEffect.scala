package com.github.kzmake.eff

import java.time.Instant

import scala.collection.concurrent.TrieMap

import cats.data.State
import org.atnos.eff.Interpret.translate
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._

trait KVStoreEffect  extends KVStoreCreation with KVStoreInterpretation
object KVStoreEffect extends KVStoreEffect

trait KVStoreTypes {
  type _kvstore[R] = KVStore |= R

  type StateKeyValue[A]  = State[TrieMap[String, (Long, Long)], A]
  type _stateKeyValue[R] = StateKeyValue /= R

  type KVStoreStack = Fx.fx2[KVStore, StateKeyValue]
}
object KVStoreTypes extends KVStoreTypes

trait KVStoreCreation extends KVStoreTypes {
  def setEx[R: _kvstore, T](key: String, ttl: Long, value: T): Eff[R, Unit] = Eff.send[KVStore, R, Unit](SetEx(key, ttl, value))
  def get[R: _kvstore, T](key: String): Eff[R, Option[T]]                   = Eff.send[KVStore, R, Option[T]](Get(key))
}

trait KVStoreInterpretation  extends KVStoreTypes {
  def run[R, U, A](
      effects: Eff[R, A],
    )(implicit
      m: Member.Aux[KVStore, R, U],
      ms: _stateKeyValue[U],
    ): Eff[U, A] =
    translate(effects)(new Translate[KVStore, U] {
      def apply[X](kv: KVStore[X]): Eff[U, X] =
        kv match {
          case SetEx(key, value, ttl) =>
            for {
              n <- Instant.now().toEpochMilli.pureEff[U]
              _ <- StateEffect.modify[U, TrieMap[String, (Long, Long)]](_ += (key -> (value, n + ttl)))
            } yield ()

          case Get(key) =>
            for {
              n <- Instant.now().toEpochMilli.pureEff[U]
              v <- StateEffect.get[U, TrieMap[String, (Long, Long)]].map(_.get(key)).map {
                case Some((value, expire_at)) if expire_at >= n => Some(value)
                case Some(_)                                    => None
                case None                                       => None
              }
            } yield v
        }
    })
}
object KVStoreInterpretation extends KVStoreInterpretation

sealed trait KVStore[A]
final case class SetEx[T](key: String, ttl: Long, value: T) extends KVStore[Unit]
final case class Get[T](key: String)                        extends KVStore[Option[T]]
