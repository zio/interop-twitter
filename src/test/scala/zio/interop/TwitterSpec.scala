package zio.interop

import com.twitter.util.{ Await, Future, FuturePool }
import java.util.concurrent.atomic.AtomicInteger
import zio._
import zio.interop.twitter._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect.{ nonFlaky, sequential }
import zio.internal._

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object TwitterSpec extends DefaultRunnableSpec {
  override val runner = {
    val ec       = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
    val executor = Executor.fromExecutionContext(Platform.defaultYieldOpCount)(ec)

    defaultTestRunner.withPlatform(_.withExecutor(executor))
  }

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
          val pool = FuturePool.interruptible(runtime.platform.executor.asECES)

          def infiniteFuture(ref: AtomicInteger): Future[Nothing] =
            pool(ref.getAndIncrement()).flatMap(_ => infiniteFuture(ref))

          for {
            ref   <- UIO(new AtomicInteger(0))
            fiber <- Task.fromTwitterFuture(infiniteFuture(ref)).fork
            _     <- fiber.interrupt
            v1    <- UIO(ref.get)
            v2    <- UIO(ref.get)
          } yield assert(v1)(equalTo(v2))
        } @@ nonFlaky(100000)
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
        } @@ nonFlaky(100000)
      )
    ) @@ sequential

  private def unsafeAwait[A](task: Task[A]): A =
    Await.result(runtime.unsafeRunToTwitterFuture(task))
}
