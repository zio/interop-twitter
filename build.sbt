import BuildHelper._

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  List(
    organization := "dev.zio",
    homepage     := Some(url("https://zio.dev")),
    licenses     := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers   := List(
      Developer(
        "jdegoes",
        "John De Goes",
        "john@degoes.net",
        url("http://degoes.net")
      ),
      Developer(
        "mijicd",
        "Dejan Mijic",
        "dmijic@acm.org",
        url("http://github.com/mijicd")
      )
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val twitter = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(stdSettings("zio-interop-twitter"))
  .settings(buildInfoSettings("zio.interop.twitter"))
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"     %% "zio"          % Zio,
      "com.twitter" %% "util-core"    % "20.12.0",
      "dev.zio"     %% "zio-test"     % Zio % Test,
      "dev.zio"     %% "zio-test-sbt" % Zio % Test
    )
  )
