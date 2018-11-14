package escale

import utest._
import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import syntax._

object SelectTest extends TestSuite {
  val tests = Tests {
    "select" - {
      val ints = Channel[Int](0)
      val strings = Channel[String](0)
      val stop = Channel[Unit](0)
      val cleaned = Channel[Int](10)

      val p0 = async {
        var done = false
        do {
          (await(Channel.select(ints, strings, stop)): @unchecked) match {
            case (`ints`, value: Int) =>
              cleaned !< value
            case (`strings`, value: String) =>
              cleaned !< value.toInt
            case (`stop`, _)    =>
              done = true
          }
        } while (!done)
        "done"
      }

      val p1 = async{
        ints !< 2
      }
      val p2 = async{
        strings !< "2"
        ints !< 1
      }
      val p3 = async{
        await(p1)
        await(p2)
        stop !< ()
      }
      p0
    }
  }

}
