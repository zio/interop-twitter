package zio.interop

import java.util.concurrent.atomic.AtomicInteger

import com.twitter.util.{ Await, Future, Promise }
import zio.Task
import zio.interop.twitter._
import zio.test._
import zio.test.Assertion._

import scala.util.{ Failure, Success, Try }

object TwitterSpec extends DefaultRunnableSpec {
  val runtime = runner.runtime

  override def spec =
    suite("TwitterSpec")(
      suite("Task.fromTwitterFuture")(
        testM("return failing `Task` if future failed.") {
          val error  = new Exception
          def future = Future.exception[Int](error)
          val task   = Task.fromTwitterFuture(future).unit

          assertM(task.either)(isLeft(equalTo(error)))
        },
        testM("return successful `Task` if future succeeded.") {
          val value  = 10
          def future = Future.value(value)
          val task   = Task.fromTwitterFuture(future).option

          assertM(task)(isSome(equalTo(value)))
        },
        testM("ensure future is interrupted together with task.") {
          val value = new AtomicInteger(0)

          val promise = new Promise[Unit] with Promise.InterruptHandler {
            override protected def onInterrupt(t: Throwable): Unit = setException(t)
          }

          def future = promise.flatMap(_ => Future(value.incrementAndGet()))

          for {
            fiber <- Task.fromTwitterFuture(future).fork
            _     <- fiber.interrupt
            _     <- Task.effect(promise.setDone())
            a     <- fiber.await
            v     <- Task.effectTotal(value.get)
          } yield assert(a.toEither)(isLeft) && assert(v)(isZero)
        }
      ),
      suite("Runtime.unsafeRunToTwitterFuture")(
        test("return successful `Future` if Task evaluation succeeded.") {
          assert(Await.result(runtime.unsafeRunToTwitterFuture(Task.succeed(2))))(equalTo(2))
        },
        test("return failed `Future` if Task evaluation failed.") {
          val error = new Exception
          val task  = Task.fail(error).unit

          val result =
            Try(Await.result(runtime.unsafeRunToTwitterFuture(task))) match {
              case Failure(exception) => Some(exception)
              case Success(_)         => None
            }

          assert(result)(isSome(equalTo(error)))
        },
        testM("ensure Task evaluation is interrupted together with Future.") {
          for {
            promise <- zio.Promise.make[Throwable, Unit]
            ref     <- zio.Ref.make(false)
            task     = promise.await *> ref.set(true)
            future  <- Task.effect(runtime.unsafeRunToTwitterFuture(task))
            _       <- Task.effect(future.raise(new Exception))
            _       <- promise.succeed(())
            value   <- ref.get
            status  <- Task.effect(Await.result(future)).either
          } yield assert(value)(isFalse) && assert(status)(isLeft)
        }
      )
    )
}
