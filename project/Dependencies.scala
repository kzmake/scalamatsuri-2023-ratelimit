import sbt._

object Dependencies {
  val ZHTTPVersion    = "0.0.4"
  val `zio-http`      = "dev.zio" %% "zio-http"         % ZHTTPVersion
  val `zio-http-test` = "dev.zio" %% "zio-http-testkit" % ZHTTPVersion % Test

  val AtnosEff    = "6.0.2"
  val `atnos-eff` = "org.atnos" %% "eff" % AtnosEff
}
