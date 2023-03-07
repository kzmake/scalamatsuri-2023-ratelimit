import Dependencies._

val rootName = "example-rate-limit"

ThisBuild / organization := "com.github.kzmake"
ThisBuild / version      := "0.1.0"

lazy val root = (project in file("."))
  .settings(name := rootName)
  .enablePlugins(JavaAppPackaging)
  .settings(BuildHelper.stdSettings)
  .settings(
    libraryDependencies ++= Seq(`atnos-eff`, `zio-http`, `zio-http-test`),
  )

addCommandAlias("fmt", "scalafmt; Test / scalafmt; fix;")
addCommandAlias("fix", "scalafix OrganizeImports; Test / scalafix OrganizeImports")
