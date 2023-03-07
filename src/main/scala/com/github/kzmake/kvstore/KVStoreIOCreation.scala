package com.github.kzmake.kvstore

import com.github.kzmake.kvstore.KVStoreIO._
import com.github.kzmake.kvstore.KVStoreIOTypes._
import org.atnos.eff.Eff

trait KVStoreIOCreation {
  def setEx[R: _kvstoreio](key: String, value: Long, ttl: Long): Eff[R, Unit] = Eff.send[KVStoreIO, R, Unit](SetEx(key, value, ttl))
  def get[R: _kvstoreio](key: String): Eff[R, Option[Long]]                   = Eff.send[KVStoreIO, R, Option[Long]](Get(key))
}
