package escale

import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros

object Macros {
  import scala.reflect.macros.blackbox._

  def goImpl[A: c.WeakTypeTag](c: Context)(body: c.Expr[A])(
      execContext: c.Expr[ExecutionContext]): c.Tree = {
    import c.universe._
    val pkg = c.mirror.staticPackage("scala.async")
    q"""$pkg.Async.async($body)($execContext)"""
  }

  def asyncTakeImpl[A: c.WeakTypeTag](c: Context)(
      channel: c.Expr[Channel[A]]): c.Tree = {
    import c.universe._
    val pkg = c.mirror.staticPackage("scala.async")
    q"""$pkg.Async.await($channel.take())"""
  }

  def asyncPutImpl[A: c.WeakTypeTag](c: Context)(value: c.Expr[A]): c.Tree = {
    import c.universe._
    val pkg = c.mirror.staticPackage("scala.async")
    q"""$pkg.Async.await(${c.prefix}.channel.put($value))"""
  }

  def selectImpl(c: Context)(channels: c.Expr[Channel[_]]*): c.Tree = {
    import c.universe._
    val pkg = c.mirror.staticPackage("scala.async")
    val Channel = c.mirror.staticModule("escale.Channel")
    q"""($pkg.Async.await($Channel.select(..$channels)): @unchecked)"""
  }

}

package object syntax {

  def chan[A](capacity: Int = 0): Channel[A] = Channel[A](capacity)

  def go[A](body: => A)(implicit execContext: ExecutionContext): Future[A] =
    macro Macros.goImpl[A]

  def !<[A](channel: Channel[A]): A = macro Macros.asyncTakeImpl[A]

  implicit class ChannelOps[A](val channel: Channel[A]) extends AnyVal {
    def !<(value: A): Unit = macro Macros.asyncPutImpl[A]
  }

  def select(channels: Channel[_]*): (Channel[_], Any) = macro Macros.selectImpl

}
