lazy val scala213 = "2.13.10"
ThisBuild / scalaVersion := scala213

ThisBuild / organization := "io.github.ramytanios"
ThisBuild / organizationName := "ramytanios"
ThisBuild / startYear := Some(2023)

ThisBuild / developers := List(
  tlGitHubDev("ramytanios", "Ramy Tanios")
)

// publish website from this branch
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
