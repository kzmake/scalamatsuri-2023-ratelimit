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
  def app(store: TrieMap[String, (Long, Long)] = TrieMap.empty): Http[Any, Nothing, Request, Response] = {
    type S = ThrottlingIOStack

    Http.collect[Request] {
      // GET /simple
      // costs:
      //   /-/perSystem -> 1
      //   /alice/perUser -> 1
      case req @ Method.GET -> !! / "simple" => simple[S](req).runThrottlingIO.runKVStore(store).runEither[Throwable].map(toResponse).run

      // GET /double
      // costs:
      //   /-/perSystem -> 3
      //   /alice/perUser -> 3
      case req @ Method.GET -> !! / "double" => double[S](req).runThrottlingIO.runKVStore(store).runEither[Throwable].map(toResponse).run

      // GET /multiple
      // costs:
      //   /-/perSystem -> 1
      //   /alice/perUser -> 1
      //   /alice/tier1 -> 2
      //   /alice/tier2 -> 1
      case req @ Method.GET -> !! / "multiple" => multiple[S](req).runThrottlingIO.runKVStore(store).runEither[Throwable].map(toResponse).run
    }
  }

  def simple[R: _throttlingio](req: Request): Eff[R, Response] = {

    def doA[R1]: Eff[R1, String] = for {
      x <- pure[R1, String]("Hello world!")
    } yield x

    for {
      u <- authenticate[R](req)
      _ <- ThrottlingIOEffect.use[R]("/-/perSystem", 1)
      _ <- ThrottlingIOEffect.use[R](s"/$u/perUser", 1)
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
      u <- authenticate[R](req)
      _ <- ThrottlingIOEffect.use[R]("/-/perSystem", 2)
      _ <- ThrottlingIOEffect.use[R](s"/$u/perUser", 2)
      x <- doA[R] // heavy
    } yield Response.text(x)
  }

  def multiple[R: _throttlingio](req: Request): Eff[R, Response] = {

    def doA[R1: _throttlingio](user: AuthenticatedUser): Eff[R1, String] = for {
      _ <- ThrottlingIOEffect.use[R1](s"/$user/tier1", 1)
      v <- pure[R1, String]("Hello")
    } yield v

    def doB[R2: _throttlingio](user: AuthenticatedUser): Eff[R2, String] = for {
      _ <- ThrottlingIOEffect.use[R2](s"/$user/tier2", 1)
      v <- pure[R2, String]("world!")
    } yield v

    def doC[R3: _throttlingio](user: AuthenticatedUser)(hello: String, world: String): Eff[R3, String] = for {
      _ <- ThrottlingIOEffect.use[R3](s"/$user/tier1", 1)
      v <- pure[R3, String](s"$hello $world")
    } yield v

    for {
      u <- authenticate[R](req)
      _ <- ThrottlingIOEffect.use[R]("/-/perSystem", 1)
      _ <- ThrottlingIOEffect.use[R](s"/$u/perUser", 1)
      x <- doA[R](u)
      y <- doB[R](u)
      z <- doC[R](u)(x, y)
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
