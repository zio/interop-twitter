package zio.interop

import com.twitter.util.{ Await, Future }
import java.util.concurrent.atomic.AtomicInteger
import zio._
import zio.duration._
import zio.interop.twitter._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.nonFlaky
import zio.test.environment.Live
import com.twitter.util.FuturePool

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
        testM("ensures future is interrupted") {
          def infiniteFuture(ref: AtomicInteger): Future[Nothing] =
            FuturePool.interruptibleUnboundedPool(ref.getAndIncrement()).flatMap(_ => infiniteFuture(ref))

          for {
            ref   <- UIO(new AtomicInteger(0))
            fiber <- Task.fromTwitterFuture(infiniteFuture(ref)).fork
            _     <- fiber.interrupt
            _     <- Live.live(clock.sleep(10.millis))
            v1    <- UIO(ref.get)
            _     <- Live.live(clock.sleep(10.millis))
            v2    <- UIO(ref.get)
          } yield assert(v1)(equalTo(v2))
        } @@ nonFlaky
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
        testM("ensures task is interrupted") {
          for {
            future <- Task(runtime.unsafeRunToTwitterFuture(UIO.never))
            _      <- Task(future.raise(new Exception))
            status <- Task(Await.result(future)).either
          } yield assert(status)(isLeft)
        } @@ nonFlaky
      )
    )

  private def unsafeAwait[A](task: Task[A]): A =
    Await.result(runtime.unsafeRunToTwitterFuture(task))
}
