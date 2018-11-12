package escale

import scala.collection.mutable
import scala.concurrent.{Future, Promise}

class Channel[A](capacity: Int) {
  require(capacity >= 0, "capacity must be >= 0")
  import Channel._

  private val puts = mutable.Queue.empty[(Handler[Unit], A)]
  private val takes = mutable.Queue.empty[Handler[A]]

  private val buffer = mutable.Queue.empty[A]

  def put(value: A): Future[Unit] = synchronized {
    val handler = new Handler[Unit]

    if (takes.size > 0) {
      val th = takes.dequeue()
      th.promise.success(value)
      handler.promise.success(())
    } else if (buffer.size < capacity) {
      buffer.enqueue(value)
      handler.promise.success(())
    } else {
      if (puts.size >= MaxOps) {
        handler.promise.failure(
          new IllegalArgumentException("Too many pending put operations."))
      } else {
        puts.enqueue(handler -> value)
      }
    }
    handler.promise.future
  }
  def take(): Future[A] = synchronized {
    val handler = new Handler[A]

    if (puts.size > 0) {
      val (ph, pd) = puts.dequeue()
      val data = if (capacity == 0) {
        pd
      } else {
        val d = buffer.dequeue()
        buffer.enqueue(pd)
        d
      }
      ph.promise.success(())
      handler.promise.success(data)
    } else if (buffer.isEmpty) {
      if (takes.size >= MaxOps) {
        handler.promise.failure(
          new IllegalArgumentException("Too many pending take operations."))
      } else {
        takes.enqueue(handler)
      }
    } else {
      handler.promise.success(buffer.dequeue())
    }
    handler.promise.future
  }

}

object Channel {
  final val MaxOps = 2

  def apply[A](capacity: Int): Channel[A] = new Channel[A](capacity)

}

class Handler[A] {
  val promise = Promise[A]
}
