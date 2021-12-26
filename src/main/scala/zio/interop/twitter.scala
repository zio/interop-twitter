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
      Task.uninterruptibleMask { restore =>
        Task(future).flatMap { future =>
          restore(Task.effectAsync { (cb: Task[A] => Unit) =>
            future.respond {
              case Return(a) => cb(Task.succeedNow(a))
              case Throw(e)  => cb(Task.fail(e))
            }
          }).onInterrupt {
            UIO(future.raise(new FutureCancelledException)) *>
              UIO.effectAsync { (cb: UIO[Unit] => Unit) =>
                future.respond {
                  case Return(_) => cb(Task.unit)
                  case Throw(_)  => cb(Task.unit)
                }
              }
          }
        }
      }
  }

  implicit class RuntimeOps[R](private val runtime: Runtime[R]) extends AnyVal {
    def unsafeRunToTwitterFuture[A](rio: RIO[R, A]): Future[A] = {
      val promise = Promise[A]()

      val interruptible =
        for {
          f <- rio.fork
          _ <- Task(promise.setInterruptHandler { case _ => runtime.unsafeRunAsync_(f.interrupt) })
          r <- f.join
        } yield r

      runtime.unsafeRunAsync(interruptible)(_.fold(c => promise.setException(c.squash), promise.setValue))

      promise
    }
  }
}
