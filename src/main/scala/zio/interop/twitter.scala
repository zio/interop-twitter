/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.interop

import com.twitter.util.{ Future, FutureCancelledException, Promise, Return, Throw }
import zio.{ RIO, Runtime, Task, UIO }

package object twitter {
  implicit class TaskObjOps(private val obj: Task.type) extends AnyVal {
    final def fromTwitterFuture[A](future: => Future[A]): Task[A] =
      toTask(Task(future))

    @deprecated("Use fromTwitterFuture[A](future: => Future[A]) instead", "v20.6.0.0-RC2")
    final def fromTwitterFuture[A](future: Task[Future[A]]): Task[A] =
      toTask(future)

    private def toTask[A](future: Task[Future[A]]): Task[A] =
      Task.uninterruptibleMask { restore =>
        future.flatMap { f =>
          restore(Task.effectAsync { cb: (Task[A] => Unit) =>
            val _ = f.respond {
              case Return(a) => cb(Task.succeed(a))
              case Throw(e)  => cb(Task.fail(e))
            }
          }).onInterrupt(UIO(f.raise(new FutureCancelledException)))
        }
      }
  }

  implicit class RuntimeOps[R](private val runtime: Runtime[R]) extends AnyVal {
    def unsafeRunToTwitterFuture[A](rio: RIO[R, A]): Future[A] = {
      val promise = Promise[A]()

      runtime.unsafeRunAsync {
        rio.fork.flatMap { f =>
          promise.setInterruptHandler {
            case _ => runtime.unsafeRunAsync_(f.interrupt)
          }
          f.join
        }
      }(_.fold(c => promise.setException(c.squash), promise.setValue))

      promise
    }
  }
}
