
import scala.quoted._

object Test {

  def main(args: Array[String]): Unit = {
    implicit val toolbox: scala.quoted.Toolbox = scala.quoted.Toolbox.make

    def test[T: Type](clazz: java.lang.Class[T]): Unit = {
      val lclazz = clazz.toExpr
      val name = '{ ($lclazz).getCanonicalName }
      println()
      println(name.show)
      println(name.run)
    }

    // classOf[Object]
    test(classOf[Object])
    test(classOf[Any])
    test(classOf[AnyRef])
    test(classOf[AnyVal])
  }

}
