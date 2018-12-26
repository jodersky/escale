package example

import escale.Channel
import scala.async.Async
import scala.async.Async._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Main extends App {

  //val t = Channel.timeout(300)
  //Await.result(t.take(), 10.seconds)

  val ch = Channel[Int](0)

//  Channel.select(
//    ch -> {(x: Int) => println("a")},
//    ch2 -> {(x: String) => println("a")}
//  )

  val p2 = async {
    var a = 0
    while ({ a = await(ch.take()); a } < 5) {
      println(a)
    }
  }

  val p1 = async {
    await(ch.put(1))
    await(ch.put(2))
    await(ch.put(2))
    await(ch.put(5))
  }

  val result = Await.result(p2, 3.seconds)
  println(result)

}

object SelectTest extends App {

  val ch = Channel[Int](0)
  val t = Channel.timeout(100)
  ch.put(2)

  val out = Channel[String](1)
//
//  Await.result(Channel.select(ch, t), 10.seconds) match {
//    case (`t`, _) => println("timeout")
//    case (`ch`, value) => println(value)
//  }

  val r = async {
    await(Channel.select(ch, t)) match {
      case (`t`, _)           => println("timeout")
      case (`ch`, value: Int) => await(out.put(value.toString)),
    }
    await(out.take())
  }
  Await.result(r, 10.seconds)
  println(r)

}

object Select2Test extends App {

  val ch = Channel[Int](0)
  val t = Channel.timeout(100)
  ch.put(2)

  val out = Channel[String](0)
  //
  //  Await.result(Channel.select(ch, t), 10.seconds) match {
  //    case (`t`, _) => println("timeout")
  //    case (`ch`, value) => println(value)
  //  }

  Channel.select2(
    t -> { u: Unit =>
      println("timeout")
    },
    ch -> { v: Int =>
      println(v); out.put(v.toString); ()
    }
  )

  val r = async {
    await(
      Channel.select2(
        t -> { u: Unit =>
          println("timeout")
        },
        out -> { s: String =>
          println(s)
        }
      ))
  }
  Await.result(r, 10.seconds)
  println(r)

}
