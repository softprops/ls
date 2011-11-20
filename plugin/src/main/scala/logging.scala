package ls

object ProcessLogging {
  val silent = new sbt.ProcessLogger {
    def info(s: => String) = ()
	    def error(s: => String) = ()
	    def buffer[T](f: => T) = f
  }
}
