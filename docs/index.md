---
id: index
title: "Introduction to ZIO Interop Twitter"
sidebar_label: "ZIO Interop Twitter"
---

This library provides capability to convert Twitter `Future` into ZIO `Task`.

### Example

```scala
import com.twitter.util.Future
import zio.Console._
import zio.interop.twitter._
import zio.{ Console, Exit, Task, URIO, ZIOAppDefault }

object Example extends ZIOAppDefault {
  def run = 
      for {
        _        <- Console.printLine("Hello! What is your name?")
        name     <- Console.readLine
        greeting <- Task.fromTwitterFuture(greet(name))
        _        <- Console.printLine(greeting)
      } yield ()

  private def greet(name: String): Future[String] = Future.value(s"Hello, $name!")

}
```
