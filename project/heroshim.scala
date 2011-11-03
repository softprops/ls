object HeroShim {
  import java.io.File
  import sbt._
  import sbt.Keys._

  object Script {
    def apply(main: String, cp: Seq[String], jvmOpts: Seq[String]) =
      """#!/bin/sh
      |
      |CLEAR="\033[0m"
      |
      |info (){
      |  COLOR="\033[0;35m"
      |  echo "$COLOR $1 $CLEAR"
      |}
      |
      |error (){
      |  COLOR="\033[0;31m"
      |  echo "$COLOR $1 $CLEAR"
      |}
      |
      |JAVA=`which java`
      |
      |CLASSPATH=%s
      |
      |info "Booting application (%s)"
      |exec $JAVA %s -classpath "$CLASSPATH" %s "$@"
      |""".stripMargin
         .format(
           cp.mkString(":"),
           main,
           jvmOpts.mkString(" "),
           main
         )
  }

   private def rootDir(state: State) =
     file(Project.extract(state).structure.root.toURL.getFile)

  private def relativeToRoot(state: State, f: File) =
    IO.relativize(rootDir(state), f)

  lazy val stage = TaskKey[Unit]("stage", "heroku hook")

  def shimNoopSettings: Seq[Setting[_]] = Seq(
    stage <<= (streams) map {
      (out) => out.log.info("noop")
    }
  )

  def shimSettings: Seq[Setting[_]] = Seq(
    stage <<= (mainClass in Compile, streams, fullClasspath in Runtime, state,
               target, javaOptions in Compile) map {
      (main, out, cp, state, target, jvmOpts) => main match {
        case Some(mainCls) =>
          val scriptBody = HeroShim.Script(mainCls, cp.files map { f =>
            relativeToRoot(state, f) match {
              case Some(rel) => rel
              case _ => f.getAbsolutePath
            }
          }, jvmOpts)
        val sf = new File(target, "hero")
        out.log.info("Writing hero file, %s" format sf)
        IO.write(sf, scriptBody)
        sf.setExecutable(true)
        sf
        case _ => sys.error("Main class required")
      }
    }
  )
}

