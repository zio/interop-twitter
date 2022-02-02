addSbtPlugin("ch.epfl.scala"  % "sbt-bloop"      % "7d2bf0af+20171218-1522")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo"  % "0.10.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"   % "2.4.6")

libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "2.3"
