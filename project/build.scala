import sbt._
import Keys._

object Build extends sbt.Build {
  import coffeescript.CoffeeScript
  import CoffeeScript._
  import less.Plugin._
  //import heroic.Plugin._


  object HeroShim {
    import java.io.File

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

    lazy val stage = TaskKey[File]("stage", "heroku hook")

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

  object Resolvers {
    val coda = "coda" at "http://repo.codahale.com"
  }

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "me.lessis",
    version := "0.1.0-SNAPSHOT"
  )

  lazy val lib = Project("library", file("library"),
                       settings = buildSettings ++ Seq(name := "ls"))


  lazy val svr = Project("server", file("server"),
                         settings = buildSettings ++ Seq(
                           name := "ls-server",
                           scalacOptions += "-deprecation",
                           libraryDependencies ++= Seq(
                             "com.codahale" %% "jerkson" % "0.5.0",
                             "net.databinder" %% "dispatch-http" % "0.8.5",
                             "net.databinder" %% "unfiltered-netty-server" % "0.5.0",
                             "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
                           )) ++ coffeeSettings ++ lessSettings ++ HeroShim.shimSettings ++ Seq(
                           (targetDirectory in Coffee) <<= (resourceManaged in Compile) { _ / "www" / "js" },
                           (resourceManaged in (Compile, LessKeys.less)) <<= (resourceManaged in Compile) { _ / "www" / "css" },
                           (LessKeys.mini in (Compile, LessKeys.less)) := true,
                           resolvers += Resolvers.coda
                         )) dependsOn(lib)

  lazy val plugin = Project("plugin", file("plugin"),
                          settings = buildSettings ++ Seq(
                            sbtPlugin := true,
                            name := "ls-sbt",
                            libraryDependencies ++= Seq(
                              "com.codahale" %% "jerkson" % "0.5.0",
                              "net.databinder" %% "dispatch-http" % "0.8.5"
                            ),
                            resolvers += Resolvers.coda
                          )) dependsOn(lib)

  lazy val tools = Project("ls-tools", file("tools"),
                         settings = buildSettings ++ Seq(
                           libraryDependencies += "com.mongodb.casbah" %% "casbah" % "2.1.5-1",
                           sbtPlugin := true
                         )) dependsOn(lib)

}
