package com.github.kzmake.api

import zio.http._
import zio.http.model._
import zio.test._

import scala.collection.concurrent.TrieMap

object HelloWorldSpec extends ZIOSpecDefault {
  override def spec =
    suite("""HelloWorldSpec""")(
      test("200 ok") {
        check(
          Gen.fromIterable(
            Seq(
              Request.get(URL(!! / "simple")),
              Request.get(URL(!! / "double")),
              Request.get(URL(!! / "multiple")),
            ),
          ),
        ) { req =>
          for {
            res <- HelloWorld.app(TrieMap.empty).runZIO(req)
          } yield assertTrue(res.status == Status.Ok)
        }
      },
      test("429 too many requests") {
        check(
          Gen.fromIterable(
            Seq(
              Request.get(URL(!! / "simple")),
              Request.get(URL(!! / "double")),
              Request.get(URL(!! / "multiple")),
            ),
          ),
        ) { req =>
          for {
            res <- HelloWorld
              .app(
                TrieMap.from(
                  Seq(
                    ("/alice/perUser", (9999999999999999L, 9999999999999999L)),
                  ),
                ),
              )
              .runZIO(req)
          } yield assertTrue(res.status == Status.TooManyRequests)
        }
      },
    )
}
