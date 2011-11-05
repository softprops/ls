package ls

object LsInit {
  def main(args: Array[String]) {
    val exit = run(args)
    System.exit(exit)
  }
  def run(args: Array[String]): Int = {
    val base = new java.io.File(args.headOption.getOrElse("."))
    if (base.isDirectory) {
      lsVersion match {
        case Some(version) =>
          println(version)
          0
        case None =>
          println("Unable to retrive current ls version")
          1
      }
    } else {
      println("Directory not found: " + base.getAbsolutePath)
      1
    }
  }
  def lsVersion = {
    import dispatch._
    val req = :/("ls.implicit.ly") / "api" / "latest" / "ls-sbt"
    val http = new Http with NoLogging
    try {
      http(req >- { js =>
        scala.util.parsing.json.JSON.parseFull(js).map {
          _.asInstanceOf[Map[String,Any]]("version")
        }
      })
    } catch {
      case e => Some(e.getMessage)
    } finally {
      http.shutdown()
    }
  }
}
class LsInit extends xsbti.AppMain {
  def run(config: xsbti.AppConfiguration) =
    new Exit(LsInit.run(config.arguments))
}
class Exit(val code: Int) extends xsbti.Exit
