lazy val scala213 = "2.13.10"
Global / semanticdbEnabled := true
Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / scalaVersion := scala213
ThisBuild / organization := "io.github.ramytanios"
ThisBuild / organizationName := "ramytanios"
ThisBuild / startYear := Some(2023)
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

lazy val ff4sVersion = "0.17.0"
lazy val circeVersion = "0.14.5"
lazy val monocleVersion = "3.2.0"
lazy val catsVersion = "2.9.0"
lazy val catsEffectVersion = "3.5.0"
lazy val fs2Version = "3.7.0"
lazy val log4CatsVersion = "2.5.0"
lazy val http4sVersion = "0.23.19"

lazy val commonSettings = Seq(
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val root = project
  .in(file("."))
  .aggregate(backend, frontend)
  .settings(crossScalaVersions := Nil)

lazy val dtos = crossProject(JSPlatform, JVMPlatform)
  .in(file("dtos"))
  .enablePlugins(GitVersioning)
  .settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-literal" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion
    )
  )

lazy val backend =
  project
    .in(file("backend"))
    .enablePlugins(GitVersioning)
    .settings(
      commonSettings,
      scalacOptions -= "-Xfatal-warnings",
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % catsVersion,
        "org.typelevel" %% "cats-free" % catsVersion,
        "org.typelevel" %% "cats-effect" % catsEffectVersion,
        "org.typelevel" %% "cats-effect-kernel" % catsEffectVersion,
        "org.typelevel" %% "cats-effect-std" % catsEffectVersion,
        "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion,
        "org.http4s" %% "http4s-dsl" % http4sVersion,
        "org.http4s" %% "http4s-circe" % http4sVersion,
        "org.http4s" %% "http4s-ember-server" % http4sVersion,
        "org.http4s" %% "http4s-ember-client" % http4sVersion,
        "co.fs2" %% "fs2-core" % fs2Version,
        "io.circe" %% "circe-generic" % circeVersion,
        "io.circe" %% "circe-literal" % circeVersion,
        "io.circe" %% "circe-parser" % circeVersion
      )
    )
    .dependsOn(dtos.jvm)

lazy val frontend =
  project
    .in(file("frontend"))
    .enablePlugins(ScalaJSPlugin, GitVersioning)
    .settings(
      commonSettings,
      scalacOptions -= "-Xfatal-warnings",
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies ++= Seq(
        "io.github.buntec" %%% "ff4s" % ff4sVersion,
        "io.circe" %%% "circe-generic" % circeVersion,
        "io.circe" %%% "circe-literal" % circeVersion,
        "io.circe" %%% "circe-parser" % circeVersion,
        "dev.optics" %%% "monocle-core" % monocleVersion,
        "dev.optics" %%% "monocle-macro" % monocleVersion
      )
    )
    .dependsOn(dtos.js)
