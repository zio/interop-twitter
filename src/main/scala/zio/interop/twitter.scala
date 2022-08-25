/*
 * Copyright 2017-2022 John A. De Goes and the ZIO Contributors
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
import zio.{ RIO, Runtime, Task, UIO, Unsafe, ZIO }

package object twitter {
  implicit class TaskObjOps(private val obj: ZIO.type) extends AnyVal {
    final def fromTwitterFuture[A](future: => Future[A]): Task[A] =
      ZIO.uninterruptibleMask { restore =>
        ZIO.attempt(future).flatMap { future =>
          restore(ZIO.async { (cb: Task[A] => Unit) =>
            future.respond {
              case Return(a) => cb(ZIO.succeedNow(a))
              case Throw(e)  => cb(ZIO.fail(e))
            }
          }).onInterrupt {
            ZIO.succeed(future.raise(new FutureCancelledException)) *>
              ZIO.async { (cb: UIO[Unit] => Unit) =>
                future.respond {
                  case Return(_) => cb(ZIO.unit)
                  case Throw(_)  => cb(ZIO.unit)
                }
              }
          }
        }
      }
  }

  implicit class RuntimeOps[R](private val unsafeAPI: Runtime[R]#UnsafeAPI) extends AnyVal {
    def runToTwitterFuture[A](rio: RIO[R, A])(implicit unsafe: Unsafe): Future[A] = {
      lazy val promise = Promise[A]()

      val interruptible =
        for {
          f <- rio.fork
          _ <- ZIO.attempt(promise.setInterruptHandler { case _ =>
                 unsafeAPI.fork(f.interrupt)
                 ()
               })
          r <- f.join
        } yield r

      unsafeAPI.fork(interruptible.foldCause(c => promise.setException(c.squash), promise.setValue))

      promise
    }
  }
}
