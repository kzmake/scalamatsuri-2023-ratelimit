package com.github.kzmake.eff

final case class Bucket(size: Int, leak: Long)

object Bucket {
  def mock(key: String): Bucket = key match {
    case "security:ipAddress/192.0.2.0"        => Bucket(size = 300, leak = 60 * 1000 / 120 /* ms */ )      // burst: 300 requests && 120 requests per minute
    case "performance:services/hoge"           => Bucket(size = 300, leak = 60 * 1000 / 120 /* ms */ )      // burst: 300 requests && 120 requests per minute
    case "monetization:users/alice"            => Bucket(size = 3, leak = 60 * 1000 / 60 /* ms */ )         // burst: 3 requests && 60 requests per minute
    case "monetization:users/alice/tiers/1111" => Bucket(size = 10, leak = 60 * 60 * 1000 / 1000 /* ms */ ) // burst: 10 requests && 1000 requests per hour
    case "monetization:users/alice/tiers/2222" => Bucket(size = 20, leak = 60 * 60 * 1000 / 2000 /* ms */ ) // burst: 20 requests && 2000 requests per hour
  }
}
