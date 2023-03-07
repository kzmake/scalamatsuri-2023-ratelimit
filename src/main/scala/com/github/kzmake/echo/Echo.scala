package com.github.kzmake.echo

import com.github.kzmake.kvstore.KVStoreIOEffect._
import com.github.kzmake.kvstore._
import com.github.kzmake.throttling.ThrottlingIOEffect._
import com.github.kzmake.throttling.ThrottlingIOTypes._
import com.github.kzmake.throttling.{ThrottlingIO, ThrottlingIOInterpreter, ThrottlingIOInterpreterImpl}
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._
import zio._
import zio.http._
import zio.http.model.Method

import scala.collection.concurrent.TrieMap

object Echo extends ZIOAppDefault with TextResponse with AuthN {
  private val store: TrieMap[String, (Long, Long)] = TrieMap.empty

  implicit private val tInt: ThrottlingIOInterpreter = new ThrottlingIOInterpreterImpl()
  implicit private val kInt: KVStoreIOInterpreter    = new KVStoreIOInterpreterImpl()

  val app = {
    type S = ThrottlingIOStack
    Http.collect[Request] {
      // GET /simple
      // costs:
      //   /-/perSystem -> 1
      //   /alice/perUser -> 1
      case req @ Method.GET -> !! / "simple" => simple[S](req).runThrottlingIO.runKVStoreIO(store).runEither[Throwable].map(toResponse).run

      // GET /heavy
      // costs:
      //   /-/perSystem -> 3
      //   /alice/perUser -> 3
      case req @ Method.GET -> !! / "heavy" => heavy[S](req).runThrottlingIO.runKVStoreIO(store).runEither[Throwable].map(toResponse).run

      // GET /complex
      // costs:
      //   /-/perSystem -> 1
      //   /alice/perUser -> 1
      //   /alice/tier1 -> 2
      //   /alice/tier2 -> 1
      case req @ Method.GET -> !! / "complex" => complex[S](req).runThrottlingIO.runKVStoreIO(store).runEither[Throwable].map(toResponse).run
    }
  }

  def simple[R: _throttlingio](req: Request): Eff[R, Response] = {

    def doA[R1]: Eff[R1, String] = for {
      x <- pure[R1, String]("Hello world!")
    } yield x

    for {
      u <- authenticate[R](req)
      _ <- ThrottlingIO.use[R]("perSystem", 1)
      _ <- ThrottlingIO.use[R](u, "perUser", 1)
      x <- doA[R]
    } yield Response.text(x)
  }

  def heavy[R: _throttlingio](req: Request): Eff[R, Response] = {

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
      _ <- ThrottlingIO.use[R]("perSystem", 3)
      _ <- ThrottlingIO.use[R](u, "perUser", 3)
      x <- doA[R] // heavy
    } yield Response.text(x)
  }

  def complex[R: _throttlingio](req: Request): Eff[R, Response] = {

    def doA[R1: _throttlingio](user: AuthenticatedUser): Eff[R1, String] = for {
      _ <- ThrottlingIO.use[R1](user, "tier1", 1)
      v <- pure[R1, String]("Hello")
    } yield v

    def doB[R2: _throttlingio](user: AuthenticatedUser): Eff[R2, String] = for {
      _ <- ThrottlingIO.use[R2](user, "tier2", 1)
      v <- pure[R2, String]("world!")
    } yield v

    def doC[R3: _throttlingio](user: AuthenticatedUser)(hello: String, world: String): Eff[R3, String] = for {
      _ <- ThrottlingIO.use[R3](user, "tier1", 1)
      v <- pure[R3, String](s"$hello $world")
    } yield v

    for {
      u <- authenticate[R](req)
      _ <- ThrottlingIO.use[R]("perSystem", 1)
      _ <- ThrottlingIO.use[R](u, "perUser", 1)
      x <- doA[R](u)
      y <- doB[R](u)
      z <- doC[R](u)(x, y)
    } yield Response.text(z)
  }

  override def run =
    ZIOAppArgs.getArgs.flatMap { _ =>
      (Server.install(app.withDefaultErrorResponse).flatMap { port =>
        Console.printLine(s"Started server on port: $port")
      } *> ZIO.never)
        .provide(ServerConfig.live(ServerConfig.default.port(3000)), Server.live)
        .exitCode
    }
}
