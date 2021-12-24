package zio.interop

import java.util.concurrent.atomic.AtomicBoolean

import com.twitter.util.{ Await, Future, Promise => TwitterPromise }
import zio._
import zio.interop.twitter._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.nonFlaky

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
                             val p = new TwitterPromise[Int]
                             p.setInterruptHandler { case _ => interrupted.set(true) }
                             p.map(_ + 1)
                           }
            fiber       <- Task.fromTwitterFuture(promise).fork
            _           <- fiber.interrupt
            interrupted <- UIO(interrupted.get)
          } yield assert(interrupted)(isTrue)
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
