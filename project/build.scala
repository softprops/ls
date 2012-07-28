import sbt._
import Keys._

object Build extends sbt.Build {
  import coffeescript.Plugin._
  import less.Plugin._

  object Resolvers {
    val coda = "coda" at "http://repo.codahale.com"
  }

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "me.lessis",
    version := "0.1.2",
    publishTo :=  Some(Resolver.file("lessis repo", new java.io.File("/var/www/repo")))
  )
  val dispatchVersion = "0.8.6"

  lazy val root = Project("root", file("."), settings = buildSettings ++ Seq(
    HeroShim.stage in Compile := {})) aggregate(
    plugin, lib
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
        "net.databinder" %% "dispatch-http" % dispatchVersion
      ),
      resolvers += Resolvers.coda
    ) ++ ScriptedPlugin.scriptedSettings ++  Seq(
      description  := "An sbt interface for ls.implicit.ly",
      homepage := Some(url("http://ls.implicit.ly/")),
      licenses := Seq(
        ("MIT", url("https://github.com/softprops/ls/blob/master/LICENSE"))
      )
    )) dependsOn(lib)

  /*lazy val tools = Project("ls-tools", file("tools"),
      settings = buildSettings ++ Seq(
        libraryDependencies += "com.mongodb.casbah" %% "casbah" % "2.1.5-1",
        sbtPlugin := true
      )) dependsOn(lib)*/

}
