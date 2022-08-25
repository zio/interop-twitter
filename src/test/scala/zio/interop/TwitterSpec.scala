package zio.interop

import com.twitter.util.{ Await, Future, FuturePool }
import zio.interop.twitter._
import zio.test.Assertion._
import zio.test.TestAspect.{ nonFlaky, sequential }
import zio.test._
import zio._

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext

object TwitterSpec extends ZIOSpecDefault {

  val customExecutor: Executor = {
    val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
    Executor.fromExecutionContext(ec)
  }

  val customExecutorLayer: ZLayer[Any, Nothing, Any] =
    Runtime.setExecutor(customExecutor) to Runtime.setBlockingExecutor(customExecutor)

  override val bootstrap: ZLayer[Scope, Any, TestEnvironment] =
    testEnvironment andTo customExecutorLayer

  def spec =
    suite("TwitterSpec")(
      suite("ZIO.fromTwitterFuture")(
        test("lifts failed futures") {
          for {
            error  <- ZIO.succeed(new Exception)
            result <- ZIO.fromTwitterFuture(Future.exception(error)).either
          } yield assert(result)(isLeft(equalTo(error)))
        },
        test("lifts successful futures") {
          for {
            value  <- ZIO.succeed(10)
            result <- ZIO.fromTwitterFuture(Future.value(value))
          } yield assert(result)(equalTo(value))
        },
        test("ensures future is interrupted") {
          val pool = FuturePool.interruptible(customExecutor.asExecutionContextExecutorService)

          def infiniteFuture(ref: AtomicInteger): Future[Nothing] =
            pool(ref.getAndIncrement()).flatMap(_ => infiniteFuture(ref))

          for {
            ref   <- ZIO.succeed(new AtomicInteger(0))
            fiber <- ZIO.fromTwitterFuture(infiniteFuture(ref)).fork
            _     <- fiber.interrupt
            v1    <- ZIO.succeed(ref.get)
            v2    <- ZIO.succeed(ref.get)
          } yield assert(v1)(equalTo(v2))
        } @@ nonFlaky(100000)
      ),
      suite("Runtime.unsafe.runToTwitterFuture")(
        test("produces successful futures if Task evaluation succeeds") {
          for {
            value  <- ZIO.succeed(10)
            result <- ZIO.attempt(unsafeAwait(ZIO.succeed(value)))
          } yield assert(result)(equalTo(value))
        },
        test("produces failed futures if Task evaluation failed") {
          for {
            error  <- ZIO.succeed(new Exception)
            result <- ZIO.attempt(unsafeAwait(ZIO.fail(error))).either
          } yield assert(result)(isLeft(equalTo(error)))
        },
        test("ensures task is interrupted") {
          for {
            future <- ZIO.attempt(unsafeRunToTwitterFuture(ZIO.never))
            _      <- ZIO.attempt(future.raise(new Exception))
            status <- ZIO.attempt(Await.result(future)).either
          } yield assert(status)(isLeft)
        } @@ nonFlaky(100000)
      )
    ) @@ sequential

  private def unsafeAwait[A](task: Task[A]): A =
    Unsafe.unsafe { implicit unsafe =>
      Await.result(runtime.unsafe.runToTwitterFuture(task.provideSomeLayer(customExecutorLayer)))
    }

  private def unsafeRunToTwitterFuture[A](task: Task[A]): Future[A] =
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.runToTwitterFuture(task.provideSomeLayer(customExecutorLayer))
    }
}
