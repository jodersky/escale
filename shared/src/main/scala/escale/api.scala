package escale

import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{Future, Promise}

class Channel[A](capacity: Int) {
  require(capacity >= 0, "capacity must be >= 0")
  import Channel._

  private val puts = mutable.Queue.empty[(Handler[Unit], A)]
  private val takes = mutable.Queue.empty[Handler[A]]

  private val buffer = mutable.Queue.empty[A]

  @tailrec final def put(handler: Handler[Unit], value: A): Unit =
    synchronized {
      if (takes.size > 0) {
        val th = takes.dequeue()
        val callback = th.commit()
        if (th.active) {

          handler.commit()(())
          callback(value)
        } else {
          put(handler, value)
        }
      } else if (buffer.size < capacity) {
        buffer.enqueue(value)
        handler.commit()(())
      } else {
        require(puts.size < MaxOps, "Too many pending put operations.")
        puts.enqueue(handler -> value)
      }
    }
  def put(value: A): Future[Unit] = {
    val p = Promise[Unit]
    put(new Handler[Unit](_ => p.success(())), value)
    p.future
  }

  def take(handler: Handler[A]): Unit = synchronized {
    if (puts.size > 0) {
      val callback = handler.commit()
      if (handler.active) {
        val (ph, pd) = puts.dequeue()
        val data = if (capacity == 0) {
          pd
        } else {
          val d = buffer.dequeue()
          buffer.enqueue(pd)
          d
        }
        ph.commit()(())
        callback(data)
      }
    } else if (buffer.isEmpty) {
      require(takes.size < MaxOps, "Too many pending take operations")
      takes.enqueue(handler)
    } else {
      val callback = handler.commit()
      if (handler.active) {
        callback(buffer.dequeue())
      }
    }
  }
  def take(): Future[A] = {
    val p = Promise[A]
    take(new Handler[A](a => p.success(a)))
    p.future
  }

}

object Channel {
  final val MaxOps = 1024

  def apply[A](capacity: Int = 0): Channel[A] = new Channel[A](capacity)

  // TODO: this currently consumes a thread for every instance
  def timeout(ms: Int): Channel[Unit] = {
    val c = new Channel[Unit](0)
    Future {
      Thread.sleep(ms)
      c.put(())
    }(scala.concurrent.ExecutionContext.global)
    c
  }

  //def select(ops: Op[_]*): Unit = ???

  def select(channels: Channel[_]*): Future[(Channel[_], Any)] = {
    val flag = new Flag
    val result = Promise[(Channel[_], Any)]
    for (ch <- channels) {
      val handler = new SelectHandler[Any](flag, v => result.success((ch, v)))
      ch.take(handler)
    }
    result.future
  }

  type Op[A] = (Channel[A], A => Unit)

  def select2(reads: Op[_]*): Future[Unit] = {
    val flag = new Flag
    val done = Promise[Unit]
    for ((ch, callback) <- reads) {
      val c = callback.andThen { _ =>
        done.success(())
        ()
      }
      val handler = new SelectHandler(flag, c)
      ch.take(handler)
    }
    done.future
  }

}
class Handler[-A](callback: A => Unit) {
  def active: Boolean = true
  def commit(): A => Unit = callback
}

class Flag {
  val active = new AtomicBoolean(true)
}
class SelectHandler[A](flag: Flag, callback: A => Unit)
    extends Handler[A](callback) {
  var _active = true
  override def active = _active
  override def commit(): A => Unit =
    if (flag.active.compareAndSet(true, false)) {
      callback
    } else {
      _active = false
      _ =>
        ()
    }

}
