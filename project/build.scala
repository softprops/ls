import sbt._
import Keys._

object Build extends sbt.Build {
  import coffeescript.Plugin._
  import less.Plugin._
  //import ls.Plugin._
  //import heroic.Plugin._

  object Resolvers {
    val coda = "coda" at "http://repo.codahale.com"
  }

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "me.lessis",
    version := "0.1.1",
    publishTo :=  Some(Resolver.file("lessis repo", new java.io.File("/var/www/repo")))
  )
  val dispatchVersion = "0.8.6"

  lazy val root = Project("root", file("."), settings = buildSettings ++ Seq(
    HeroShim.stage in Compile := {})) aggregate(
    svr, plugin, lib, app
  ) 

  lazy val lib = Project("library", file("library"),
    settings = buildSettings ++ Seq(name := "ls"))

  lazy val svr = Project("server", file("server"),
    settings = buildSettings ++ Seq(
      name := "ls-server",
      scalacOptions += "-deprecation",
      libraryDependencies ++= Seq(
        "com.codahale" %% "jerkson" % "0.5.0",
        "net.databinder" %% "dispatch-http" % dispatchVersion,
        "net.databinder" %% "unfiltered-netty-server" % "0.5.1",
        "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
      )) ++ coffeeSettings ++ lessSettings ++ HeroShim.shimSettings ++ /* heroicSettings ++ */
         Seq(
           (resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (resourceManaged in Compile) {
             _ / "www" / "js"
           },
           (resourceManaged in (Compile, LessKeys.less)) <<= (resourceManaged in Compile) {
             _ / "www" / "css"
           },
           (LessKeys.mini in (Compile, LessKeys.less)) := true,
           resolvers += Resolvers.coda
         )) dependsOn(lib)

  lazy val plugin = Project("plugin", file("plugin"),
    settings = buildSettings ++ Seq(
      sbtPlugin := true,
      name := "ls-sbt",
      libraryDependencies ++= Seq(
        "com.codahale" %% "jerkson" % "0.5.0",
        "net.databinder" %% "dispatch-http" % dispatchVersion,
        "me.lessis" %% "ls" % "0.1.0"
      ),
      resolvers += Resolvers.coda
    ) ++ ScriptedPlugin.scriptedSettings /*++ lsSettings ++ Seq(
      description in LsKeys.lsync := "An sbt interface for ls.implicit.ly",
      LsKeys.tags in LsKeys.lsync := Seq("ls", "plugin", "sbt"),
      externalResolvers in LsKeys.lsync := Seq(
        "less is" at "http://repo.lessis.me",
        "coda" at "http://repo.codahale.com"
      ),
      homepage in LsKeys.lsync := Some(url("http://ls.implicit.ly/")),
      licenses in LsKeys.lsync := Seq(
        ("MIT", url("https://github.com/softprops/ls/blob/master/LICENSE"))
      )
    )*/) dependsOn(lib) /* here will not work in sbt as it generates pom with a sources classifier,
      references to class with in at that point result in class not found exceptions. sbt xsbt issue #257 */

  lazy val app = Project("app", file("app"),
    settings = buildSettings ++ 
      conscript.Harness.conscriptSettings ++ Seq(
        name := "ls-app",
        libraryDependencies += 
        "net.databinder" %% "dispatch-http" % dispatchVersion
      )
    ) dependsOn(lib)

  /*lazy val tools = Project("ls-tools", file("tools"),
      settings = buildSettings ++ Seq(
        libraryDependencies += "com.mongodb.casbah" %% "casbah" % "2.1.5-1",
        sbtPlugin := true
      )) dependsOn(lib)*/

}
