# Interop Twitter

![CI][Badge-CI]
[![Releases][Badge-SonatypeReleases]][Link-SonatypeReleases]
[![Snapshots][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

This library provides capability to convert Twitter `Future` into ZIO `Task`.

### Example

```scala
import com.twitter.util.Future
import zio.{ App, Task }
import zio.console._
import zio.interop.twitter._

object Example extends App {
  def run(args: List[String]) = {
    val program =
      for {
        _        <- putStrLn("Hello! What is your name?")
        name     <- getStrLn
        greeting <- Task.fromTwitterFuture(greet(name))
        _        <- putStrLn(greeting)
      } yield ()

    program.fold(_ => 1, _ => 0)
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
