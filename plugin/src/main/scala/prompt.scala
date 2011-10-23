package ls

object Prompt {
  val Yes = Seq("y", "yea", "yes", "yep")
  val No = Seq("n", "nah", "no", "nope")
  def ask[T](question: String)(f: String => T): T = f(sbt.SimpleReader.readLine(question).get)
}
