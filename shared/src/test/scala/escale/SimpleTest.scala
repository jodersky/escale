package escale

import scala.async.Async.{async, await}
import utest._
import scala.concurrent.ExecutionContext.Implicits.global

object SimpleTest extends TestSuite {
  val tests = Tests{
    "put and take" - {
      val ch = Channel[Int](0)
      val p1 = async{
        await(ch.put(1))
        await(ch.put(2))
        await(ch.put(3))
        await(ch.put(4))
      }
      async{
        await(ch.take())
        await(ch.take())
        await(ch.take())
        await(ch.take())
      }
      p1
    }
    "put and take while"- {
      val ch = Channel[Int](0)
      ch.put(1)

      val p1 = async {
        await(ch.put(2))
        await(ch.put(3))
        await(ch.put(4))
        await(ch.put(5))
      }

      val p2 = async {
        var sum = 0
        var a = 0
        while ({ a = await(ch.take()); a } < 5) {
          sum += a
        }
        assert(sum == 10)
      }
      p2
    }
  }
}