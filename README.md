# Interop Twitter

[![CircleCI][ci-badge]][ci-url]

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
        greeting <- Task.fromTwitterFuture(Task(greet(name)))
        _        <- putStrLn(greeting)
      } yield ()

    program.fold(_ => 1, _ => 0)
  }

  private def greet(name: String): Future[String] = Future.value(s"Hello, $name!")
}
```

[ci-badge]: https://circleci.com/gh/zio/interop-twitter/tree/master.svg?style=svg
[ci-url]: https://circleci.com/gh/zio/interop-twitter/tree/master
