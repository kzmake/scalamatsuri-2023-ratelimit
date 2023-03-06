package com.github.kzmake.echo

import com.github.kzmake.throttling.ThrottlingIO
import com.github.kzmake.throttling.ThrottlingIOEffect._
import com.github.kzmake.throttling.ThrottlingIOInterpreterImpl
import com.github.kzmake.throttling.ThrottlingIOTypes._
import zio._
import zio.http._
import zio.http.model.Method
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.syntax.all._

object Echo extends ZIOAppDefault {
  implicit private val int: ThrottlingIOInterpreterImpl = new ThrottlingIOInterpreterImpl()

  val app = Http.collect[Request] {
    // GET /simple
    case Method.GET -> !! / "simple"  => simple
    // GET /complex
    case Method.GET -> !! / "complex" => complex
    // GET /heavy
    case Method.GET -> !! / "heavy"   => heavy
  }

  def simple: Response = {
    type S = ThrottlingIOStack

    def doA[R: _throttlingio](): Eff[R, String] = for {
      _ <- ThrottlingIO.use[R]("api", 3)
      x <- pure[R, String]("Hello world!")
    } yield x

    val effects = for {
      x <- doA[S]()
    } yield Response.text(x)

    effects
      .runThrottlingIO
      .run
  }

  def heavy: Response = {
    type S = NoFx

    def doA[R](): Eff[R, String] = {
      def rot13(v: String): String = v.map {
        case c if c.toString.matches("[A-Ma-m]") => (c.toInt + 13).toChar
        case c if c.toString.matches("[N-Zn-z]") => (c.toInt - 13).toChar
        case c                                   => c
      }

      pure[R, String](rot13("Uryyb jbeyq!"))
    }

    val effects = for {
      x <- doA[S]()
    } yield Response.text(x)

    effects.run
  }

  def complex: Response = {
    type S = NoFx

    def doA[R](): Eff[R, String]                             = pure[R, String]("Hello")
    def doB[R](): Eff[R, String]                             = pure[R, String]("world!")
    def doC[R](hello: String, world: String): Eff[R, String] = pure[R, String](s"$hello $world")

    val effects = for {
      x <- doA[S]()
      y <- doB[S]()
      z <- doC[S](x, y)
    } yield Response.text(z)

    effects.run
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
