package ls

object Clock {
  import System.{currentTimeMillis => now }
  def apply[T](what: String, l: Logger)(f: => T) = {
    val then = now
    try { f }
    finally { l.info("%s took %d ms" format(what, now - then)) }
  }
}
