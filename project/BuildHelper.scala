import sbt._
import Keys._

import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbtbuildinfo._
import BuildInfoKeys._

object BuildHelper {
  private val stdOptions = Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked"
  )

  private val std2xOptions = Seq(
    "-Xfatal-warnings",
    "-language:higherKinds",
    "-language:existentials",
    "-explaintypes",
    "-Yrangepos",
    "-Xsource:2.13",
    "-Xlint:_,-type-parameter-shadow",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )

  val buildInfoSettings = Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, isSnapshot),
    buildInfoPackage := "zio",
    buildInfoObject := "BuildInfo"
  )

  val optimizerOptions =
    Seq("-opt:l:inline", "-opt-inline-from:zio.internal.**")

  def extraOptions(scalaVersion: String) =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) =>
        std2xOptions ++ optimizerOptions
      case Some((2, 12)) =>
        Seq(
          "-opt-warnings",
          "-Ywarn-extra-implicit",
          "-Ywarn-unused:_,imports",
          "-Ywarn-unused:imports",
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Xfuture"
        ) ++ std2xOptions ++ optimizerOptions
      case Some((2, 11)) =>
        Seq(
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Xexperimental",
          "-Ywarn-unused-import",
          "-Xfuture"
        ) ++ std2xOptions
      case _             => Seq.empty
    }

  def stdSettings(prjName: String) =
    Seq(
      name := s"$prjName",
      scalacOptions := stdOptions,
      crossScalaVersions := Seq("2.13.3", "2.12.12", "2.13.5"),
      scalaVersion in ThisBuild := crossScalaVersions.value.head,
      scalacOptions := stdOptions ++ extraOptions(scalaVersion.value),
      parallelExecution in Test := true,
      incOptions ~= (_.withLogRecompileOnMacro(false))
    )
}
