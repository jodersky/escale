package escale

import utest._
import scala.concurrent.ExecutionContext.Implicits.global
import escale.syntax._
import scala.concurrent.Future

object SyntaxTest extends TestSuite {
  val tests = Tests {
    "!< and !<" - {
      val ch1 = chan[Int]()
      val ch2 = chan[Int](1)
      go {
        ch1 !< 1
        ch1 !< 2
        ch1 !< 3
      }
      go {
        var sum = 0
        sum += !<(ch1)
        sum += !<(ch1)
        sum += !<(ch1)
        ch2 !< 4
        sum += !<(ch2)
        assert(sum == 10)
      }
    }
    "select syntax" - {
      def run(): Future[String] = go {
        val Ch1 = chan[Int]()
        val Ch2 = chan[Int]()

        go {/*Thread.sleep(1);*/ Ch1 !< 1}
        go {/*Thread.sleep(1);*/ Ch2 !< 1}

        select(Ch1, Ch2) match {
          case (Ch1, _) => "ch1 was first"
          case (Ch2, _) => "ch2 was first"
        }
      }
      run()
    }
  }
}
