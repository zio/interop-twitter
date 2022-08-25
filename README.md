# Interop Twitter

[![Project stage][Stage]][Stage-Page]
![CI][Badge-CI]
[![Releases][Badge-SonatypeReleases]][Link-SonatypeReleases]
[![Snapshots][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

This library provides capability to convert Twitter `Future` into ZIO `Task`.

### Example

```scala
import com.twitter.util.Future
import zio.Console._
import zio.interop.twitter._
import zio.{ Console, Exit, ZIO, URIO, ZIOAppDefault }

object Example extends ZIOAppDefault {
  def run: URIO[Console, Exit[Throwable, Unit]] = {
    val program =
      for {
        _        <- printLine("Hello! What is your name?")
        name     <- readLine
        greeting <- ZIO.fromTwitterFuture(greet(name))
        _        <- printLine(greeting)
      } yield ()

    program.exit
  }

  private def greet(name: String): Future[String] = Future.value(s"Hello, $name!")

}
```

[Badge-CI]: https://github.com/zio/interop-twitter/workflows/CI/badge.svg
[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/dev.zio/zio-interop-twitter_2.12.svg "Sonatype Releases"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/dev.zio/zio-interop-twitter_2.12.svg "Sonatype Snapshots"
[Link-Circle]: https://circleci.com/gh/zio/interop-twitter/tree/master
[Link-SonatypeReleases]: https://oss.sonatype.org/content/repositories/releases/dev/zio/zio-interop-twitter_2.12/ "Sonatype Releases"
[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/dev/zio/zio-interop-twitter_2.12/ "Sonatype Snapshots"
[Stage]: https://img.shields.io/badge/Project%20Stage-Production%20Ready-brightgreen.svg
[Stage-Page]: https://github.com/zio/zio/wiki/Project-Stages
