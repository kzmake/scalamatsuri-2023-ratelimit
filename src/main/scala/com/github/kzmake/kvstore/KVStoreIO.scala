package com.github.kzmake.kvstore

trait KVStoreIO[+A]

object KVStoreIO extends KVStoreIOCreation {
  case class SetEx(key: String, value: Long, ttl: Long) extends KVStoreIO[Unit]
  case class Get(key: String)                           extends KVStoreIO[Option[Long]]
}
