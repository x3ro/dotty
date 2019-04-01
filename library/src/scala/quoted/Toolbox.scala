package scala.quoted

import scala.annotation.implicitNotFound

@implicitNotFound("Could not find implicit quoted.Toolbox.\n\nDefault toolbox can be instantiated with:\n  `implicit val toolbox: scala.quoted.Toolbox = scala.quoted.Toolbox.make`\n\n")
trait Toolbox {
  def run[T](expr: Expr[T]): T
  def show[T](expr: Expr[T]): String
  def show[T](tpe: Type[T]): String
}

object Toolbox {

  /** Create a new instance of the toolbox.
    *
    * This instance of the toolbox tries to recover the classloader of the application based on
    * the classloader of the class that call make. This may not always be the correct classloader,
    * in which case the classloader must be passed explicitly.
    *
    * @param settings toolbox settings
    * @return A new instance of the toolbox
    */
  @forceInline def make(implicit settings: Settings): Toolbox = {
    // Get the name of the class at call site
    val className = new Throwable().getStackTrace.head.getClassName
    // We inline to make forName use the classloader of the class at call site
    val clazz = Class.forName(className)
    val cl = clazz.getClassLoader
    make(cl)
  }

  /** Create a new instance of the toolbox using the the classloader of the application.
    *
    * @param appClassloader classloader of the application that generated the quotes
    * @param settings toolbox settings
    * @return A new instance of the toolbox
    */
  def make(appClassloader: ClassLoader)(implicit settings: Settings): Toolbox = {
    try {
      val toolboxImplCls = appClassloader.loadClass("dotty.tools.dotc.quoted.ToolboxImpl")
      val makeMeth = toolboxImplCls.getMethod("make", classOf[Settings], classOf[ClassLoader])
      makeMeth.invoke(null, settings, appClassloader).asInstanceOf[Toolbox]
    }
    catch {
      case ex: ClassNotFoundException =>
        throw new ToolboxNotFoundException(
          s"""Could not load the Toolbox class `${ex.getMessage}` from the JVM classpath. Make sure that the compiler is on the JVM classpath.""",
          ex
        )
    }
  }

  /** Setting of the Toolbox instance. */
  case class Settings private (outDir: Option[String], showRawTree: Boolean, compilerArgs: List[String], color: Boolean)

  object Settings {

    implicit def default: Settings = make()

    /** Make toolbox settings
     *  @param outDir Output directory for the compiled quote. If set to None the output will be in memory
     *  @param color Print output with colors
     *  @param showRawTree Do not remove quote tree artifacts
     *  @param compilerArgs Compiler arguments. Use only if you know what you are doing.
     */
    def make( // TODO avoid using default parameters (for binary compat)
      color: Boolean = false,
      showRawTree: Boolean = false,
      outDir: Option[String] = None,
      compilerArgs: List[String] = Nil
    ): Settings =
      new Settings(outDir, showRawTree, compilerArgs, color)
  }

  class ToolboxNotFoundException(msg: String, cause: ClassNotFoundException) extends Exception(msg, cause)
}
