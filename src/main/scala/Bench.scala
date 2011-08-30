package ls

object Bench {
  def apply[T](name: String)(f: => T) = {
    val s = System.currentTimeMillis
    try {
      f
    } finally {
      println("%s took %sms" format(name, System.currentTimeMillis - s))
    }
  }
}
