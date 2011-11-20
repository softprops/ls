package ls

trait Wheel[T] {
  def turn: Wheel[T]
  def get: T
}

object Wheels {
  val colors = "\033[0;32m" :: "\033[0;33m" :: "\033[0;34m" :: "\033[0;35m" :: "\033[0;36m" ::  Nil

  def colorWheel(opts: Seq[String]): Wheel[String] = new Wheel[String] {
    def turn = opts match {
      case head :: tail => colorWheel(tail ::: head :: Nil)
      case head => colorWheel(head)
    }
    def get = opts(0)
  }

  def shuffle = util.Random.shuffle(colors)

  def default = colorWheel(colors)
}
