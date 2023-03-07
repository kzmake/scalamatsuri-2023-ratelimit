import sbt._

object Dependencies {
  val ZHTTPVersion    = "0.0.4"
  val `zio-http`      = "dev.zio" %% "zio-http"         % ZHTTPVersion
  val `zio-http-test` = "dev.zio" %% "zio-http-testkit" % ZHTTPVersion % Test

  val AtnosEff    = "6.0.2"
  val `atnos-eff` = "org.atnos" %% "eff" % AtnosEff

  val ZIOVersion          = "2.0.10"
  val zio                 = "dev.zio" %% "zio"               % ZIOVersion
  val `zio-test`          = "dev.zio" %% "zio-test"          % ZIOVersion % Test
  val `zio-test-sbt`      = "dev.zio" %% "zio-test-sbt"      % ZIOVersion % Test
  val `zio-test-magnolia` = "dev.zio" %% "zio-test-magnolia" % ZIOVersion % Test
}
