package zio.interop

import java.util.concurrent.atomic.AtomicBoolean

import com.twitter.util.{ Await, Future, Promise }
import zio.{ Task, UIO }
import zio.interop.twitter._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.flaky

object TwitterSpec extends DefaultRunnableSpec {
  val runtime = runner.runtime

  def spec =
    suite("TwitterSpec")(
      suite("Task.fromTwitterFuture")(
        testM("lifts failed futures") {
          for {
            error  <- UIO(new Exception)
            result <- Task.fromTwitterFuture(Future.exception(error)).either
          } yield assert(result)(isLeft(equalTo(error)))
        },
        testM("lifts successful futures") {
          for {
            value  <- UIO(10)
            result <- Task.fromTwitterFuture(Future.value(value))
          } yield assert(result)(equalTo(value))
        },
        testM("ensures future is interrupted together with task") {
          for {
            interrupted <- UIO(new AtomicBoolean(false))
            promise     <- UIO {
                             val p = new Promise[Int]
                             p.setInterruptHandler { case _ => interrupted.set(true) }
                             p.map(_ + 1)
                           }
            fiber       <- Task.fromTwitterFuture(promise).fork
            _           <- fiber.interrupt
            interrupted <- UIO(interrupted.get)
          } yield assert(interrupted)(isTrue)
        }
      ),
      suite("Runtime.unsafeRunToTwitterFuture")(
        testM("produces successful futures if Task evaluation succeeds") {
          for {
            value  <- UIO(10)
            result <- Task(unsafeAwait(UIO(value)))
          } yield assert(result)(equalTo(value))
        },
        testM("produces failed futures if Task evaluation failed") {
          for {
            error  <- UIO(new Exception)
            result <- Task(unsafeAwait(Task.fail(error))).either
          } yield assert(result)(isLeft(equalTo(error)))
        },
        testM("ensures Task evaluation is interrupted together with Future.") {
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
        } @@ flaky
      )
    )

  private def unsafeAwait[A](task: Task[A]): A =
    Await.result(runtime.unsafeRunToTwitterFuture(task))
}
