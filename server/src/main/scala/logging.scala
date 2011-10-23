package ls

trait Logger {
  def info(s: => String): Unit
  def warn(s: => String): Unit
  def error(s: => String, t: Option[Throwable] = None): Unit
}

trait Logged {
  def log: Logger = StdOutLogger
}

object StdOutLogger extends Logger {
  def info(s: => String) = println(s)
  def warn(s: => String) = println(s)
  def error(s: => String, t: Option[Throwable] = None) =
    Console.err.println(s, t.map(_.getMessage))
}


