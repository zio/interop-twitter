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

          val task =
            (for {
              fiber <- Task.fromTwitterFuture(future).fork
              _     <- fiber.interrupt
              _     <- Task.effect(promise.setDone())
              a     <- fiber.await
            } yield a).fold(_ => false, exit => exit.toEither.isLeft)

          task.map(b => assert(b)(isTrue) && assert(value.get())(equalTo(0)))
        }
      ),
      suite("Runtime.unsafeRunToTwitterFuture")(
        test("return successful `Future` if Task evaluation succeeded.") {
          assert(Await.result(runtime.unsafeRunToTwitterFuture(Task.succeed(2))))(equalTo(2))
        },
        test("return failed `Future` if Task evaluation failed.") {
          val e      = new Throwable
          val task   = Task.fail(e).unit
          val result =
            Try(Await.result(runtime.unsafeRunToTwitterFuture(task))) match {
              case Failure(exception) => Some(exception)
              case Success(_)         => None
            }

          assert(result)(isSome(equalTo(e)))
        },
        testM("ensure Task evaluation is interrupted together with Future.") {
          val value = new AtomicInteger(0)
          val ex    = new Exception

          val task =
            for {
              promise <- zio.Promise.make[Throwable, Int]
              t        = promise.await *> Task.effectTotal(value.incrementAndGet())
              future   = runtime.unsafeRunToTwitterFuture(t)
              _        = future.raise(ex)
              _       <- promise.succeed(1)
            } yield future

          assertM(task.map(Await.result(_)).run)(isInterrupted).map(_ && assert(value.get)(equalTo(0)))
        }
      )
    )
}
