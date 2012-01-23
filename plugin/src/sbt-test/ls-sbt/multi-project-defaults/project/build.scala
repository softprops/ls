object MultiProject extends sbt.Build {
  import sbt._
  import sbt.Keys._
  import ls.Plugin._

  def scriptedExtras: Seq[Setting[_]] = Seq(
    InputKey[Unit]("contents") <<= inputTask {
      (argsTask: TaskKey[Seq[String]]) =>
        (argsTask, streams) map {
          (args, out) =>
            args match {
              case Seq(given, expected) =>
                if(IO.read(file(given)).trim.equals(
                  IO.read(file(expected)).trim)) out.log.debug(
                  "Contents match"
                )
                else sys.error(
                  "Contents of (%s)\n'%s' does not match (%s)\n'%s'" format(
                    given, IO.read(file(given)).trim, expected, IO.read(file(expected)).trim
                  )
                )
              case _ => sys.error("usage: contents path/to/actual path/to/expected")
            }
        }
    }
  )

  def shared: Seq[Setting[_]] = Defaults.defaultSettings ++ scriptedExtras ++
    lsSettings ++ Seq(
    // here we apply settings that ls-sbt `should` default to
    // when not scoped to LsKeys.lsync
    licenses := Seq(
      ("MIT", url("http://github.com"))
    ),
    homepage :=
      Some(new java.net.URL("http://github.com")),
    // let's not publish to the live server by accident for tests :)
    LsKeys.host in LsKeys.lsync := "http://localhost:5000"
  )

  lazy val root = Project("root", file("."), settings = shared ++ Seq(
    LsKeys.skipWrite := true // don't gen a :version.json for root
  )) aggregate(
    a, b, c
  )
 
  lazy val a = Project("a", file("a"), settings = shared)

  lazy val b = Project("b", file("b"), settings = shared)

  lazy val c = Project("c", file("c"), settings = shared)
}
