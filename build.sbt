inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://zio.dev")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "jdegoes",
        "John De Goes",
        "john@degoes.net",
        url("http://degoes.net")
      )
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

pgpPublicRing := file("/tmp/public.asc")
pgpSecretRing := file("/tmp/secret.asc")
releaseEarlyWith := SonatypePublisher
scmInfo := Some(
  ScmInfo(url("https://github.com/zio/interop-twitter/"), "scm:git:git@github.com:zio/interop-twitter.git")
)

lazy val twitter = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, isSnapshot),
    buildInfoPackage := "zio",
    buildInfoObject := "BuildInfo"
  )
  .settings(
    crossScalaVersions := Seq("2.12.8", "2.11.12"),
    name := "zio-interop-twitter",
    libraryDependencies ++= Seq(
      "dev.zio"     %% "scalaz-zio"  % "1.0-RC6",
      "com.twitter" %% "util-core"   % "19.5.1",
      "org.specs2"  %% "specs2-core" % "4.5.1" % Test
    )
  )
