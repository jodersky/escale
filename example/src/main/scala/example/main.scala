package example

import escale.Channel
import scala.async.Async._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends App {

  val ch = Channel[Int](0)

  val p1 = async {
    await(ch.put(1))
    await(ch.put(2))
    await(ch.put(2))
    await(ch.put(5))
  }

  val p2 = async {
    await(ch.take())
    await(ch.take())
    await(ch.take())
    await(ch.take())
  }

  val result = Await.result(p2, 3.seconds)
  println(result)

}
