package com.github.kzmake.eff

final case class Bucket(size: Int, leak: Long)

object Bucket {
  def mock(key: String): Bucket = key match {
    case "/services/hoge"          => Bucket(300, 60 * 1000 / 120 /* ms */ )      // burst: 300 requests && 120 requests per minute
    case "/users/alice"            => Bucket(3, 60 * 1000 / 60 /* ms */ )         // burst: 3 requests && 60 requests per minute
    case "/users/alice/tiers/1111" => Bucket(10, 60 * 60 * 1000 / 1000 /* ms */ ) // burst: 10 requests && 1000 requests per hour
    case "/users/alice/tiers/2222" => Bucket(20, 60 * 60 * 1000 / 2000 /* ms */ ) // burst: 20 requests && 2000 requests per hour
  }
}
