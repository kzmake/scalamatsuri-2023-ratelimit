package com.github.kzmake.kvstore

import com.github.kzmake.kvstore.KVStoreIO._
import com.github.kzmake.kvstore.KVStoreIOTypes._
import org.atnos.eff.Interpret.translate
import org.atnos.eff._
import org.atnos.eff.syntax.all._
import java.time.Instant
import scala.collection.concurrent.TrieMap

class KVStoreIOInterpreterImpl() extends KVStoreIOInterpreter {
  override def run[R, U, A](
      effects: Eff[R, A],
    )(implicit
      m: Member.Aux[KVStoreIO, R, U],
      ms: _stateKeyValue[U],
    ): Eff[U, A] =
    translate(effects)(new Translate[KVStoreIO, U] {
      def apply[X](kv: KVStoreIO[X]): Eff[U, X] =
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
