addSbtPlugin("ch.epfl.scala"  % "sbt-bloop"      % "1.4.11")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo"  % "0.10.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"   % "2.4.4")
addSbtPlugin("ch.epfl.scala"  % "sbt-scalafix"   % "0.9.31")

libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "2.3"
