addSbtPlugin("ch.epfl.scala"  % "sbt-bloop"      % "1.5.10")
addSbtPlugin("com.eed3si9n"   % "sbt-buildinfo"  % "0.10.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"   % "2.5.2")

libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "2.3"
