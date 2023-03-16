package com.github.kzmake.api

import scala.collection.concurrent.TrieMap

import com.github.kzmake.eff.ThrottlingIOEffect
import com.github.kzmake.eff.all._
import com.github.kzmake.eff.syntax.all._
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._
import zio._
import zio.http._
import zio.http.model.Method

object HelloWorld extends ZIOAppDefault with TextResponse with AuthN {
  type S = ThrottlingIOStack

  def app(store: TrieMap[String, (Long, Any)] = TrieMap.empty): Http[Any, Nothing, Request, Response] = {
    type S = ThrottlingIOStack

    Http.collect[Request] {
      // GET /single
      // costs:
      //   /-/perSystem -> 1
      //   /alice/perUser -> 1
      case req @ Method.GET -> !! / "single" => single[S](req).runThrottlingIO.runKVStore(store).runEither[Throwable].map(toResponse).run

      // GET /double
      // costs:
      //   /-/perSystem -> 3
      //   /alice/perUser -> 3
      case req @ Method.GET -> !! / "double" => double[S](req).runThrottlingIO.runKVStore(store).runEither[Throwable].map(toResponse).run

      // GET /multiple
      // requests:
      //   /services/hoge -> 1 cell
      //   /users/alice -> 1 cell
      //   /users/alice/tiers/1111 -> 2 cells
      //   /users/alice/tiers/2222 -> 1 cell
      case req @ Method.GET -> !! / "multiple" => multiple[S](req).runThrottlingIO.runKVStore(store).runEither[Throwable].map(toResponse).run
    }
  }

  def single[R: _throttlingio](req: Request): Eff[R, Response] = {

    def doA[R1]: Eff[R1, String] = for {
      x <- pure[R1, String]("Hello world!")
    } yield x

    for {
      user <- authenticate[R](req)

      _ <- ThrottlingIOEffect.request[R]("/services/hoge", 1)
      _ <- ThrottlingIOEffect.request[R](s"/users/$user", 1)

      x <- doA[R]
    } yield Response.text(x)
  }

  def double[R: _throttlingio](req: Request): Eff[R, Response] = {

    def doA[R1]: Eff[R1, String] = {
      def rot13(v: String): String = v.map {
        case c if c.toString.matches("[A-Ma-m]") => (c.toInt + 13).toChar
        case c if c.toString.matches("[N-Zn-z]") => (c.toInt - 13).toChar
        case c                                   => c
      }

      for {
        v <- rot13("Uryyb jbeyq!").pureEff[R1]
      } yield v
    }

    for {
      user <- authenticate[R](req)

      _ <- ThrottlingIOEffect.request[R]("/services/hoge", 2)
      _ <- ThrottlingIOEffect.request[R](s"/users/$user", 2)

      x <- doA[R] // heavy
    } yield Response.text(x)
  }

  def doA[R1: _throttlingio](user: AuthenticatedUser): Eff[R1, String] = for {
    _ <- ThrottlingIOEffect.request[R1](s"/users/$user/tiers/1111", 1)
    v <- pure[R1, String]("Hello")
  } yield v

  def doB[R2: _throttlingio](user: AuthenticatedUser): Eff[R2, String] = for {
    _ <- ThrottlingIOEffect.request[R2](s"/users/$user/tiers/2222", 1)
    v <- pure[R2, String]("world!")
  } yield v

  def doC[R3: _throttlingio](user: AuthenticatedUser)(hello: String, world: String): Eff[R3, String] = for {
    _ <- ThrottlingIOEffect.request[R3](s"/users/$user/tiers/1111", 1)
    v <- pure[R3, String](s"$hello $world")
  } yield v

  def multiple[R: _throttlingio](req: Request): Eff[R, Response] = {
    def doA[R1: _throttlingio](user: AuthenticatedUser): Eff[R1, String] = for {
      _ <- ThrottlingIOEffect.request[R1](s"/users/$user/tiers/1111", 1)
      v <- pure[R1, String]("Hello")
    } yield v

    def doB[R2: _throttlingio](user: AuthenticatedUser): Eff[R2, String] = for {
      _ <- ThrottlingIOEffect.request[R2](s"/users/$user/tiers/2222", 1)
      v <- pure[R2, String]("world!")
    } yield v

    def doC[R3: _throttlingio](user: AuthenticatedUser)(hello: String, world: String): Eff[R3, String] = for {
      _ <- ThrottlingIOEffect.request[R3](s"/users/$user/tiers/1111", 1)
      v <- pure[R3, String](s"$hello $world")
    } yield v

    for {
      user <- authenticate[R](req)

      _ <- ThrottlingIOEffect.request[R]("/services/hoge", 1)
      _ <- ThrottlingIOEffect.request[R](s"/users/$user", 1)

      x <- doA[R](user)       // with ThrottlingIOEffect.request[R](1, s"/users/$user/tiers/1111")
      y <- doB[R](user)       // with ThrottlingIOEffect.request[R](1, s"/users/$user/tiers/2222")
      z <- doC[R](user)(x, y) // with ThrottlingIOEffect.request[R](1, s"/users/$user/tiers/1111")
    } yield Response.text(z)
  }

  override def run =
    ZIOAppArgs.getArgs.flatMap { _ =>
      (Server.install(app().withDefaultErrorResponse).flatMap { port =>
        Console.printLine(s"Started server on port: $port")
      } *> ZIO.never)
        .provide(ServerConfig.live(ServerConfig.default.port(3000)), Server.live)
        .exitCode
    }
}
