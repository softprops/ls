import sbt._
import Keys._

object Build extends sbt.Build {
  import coffeescript.CoffeeScript
  import CoffeeScript._
  import less.Plugin._
  //import heroic.Plugin._

  object Resolvers {
    val coda = "coda" at "http://repo.codahale.com"
  }

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "me.lessis",
    version := "0.1.0-SNAPSHOT"
  )

  lazy val root = Project("root", file("."), settings = buildSettings ++ Seq(
    HeroShim.stage in Compile := {})) aggregate(
    svr, plugin, lib
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
                           )) ++ coffeeSettings ++ lessSettings ++ HeroShim.shimSettings ++ /* heroicSettings ++ */
                          Seq(
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

  /*lazy val tools = Project("ls-tools", file("tools"),
                         settings = buildSettings ++ Seq(
                           libraryDependencies += "com.mongodb.casbah" %% "casbah" % "2.1.5-1",
                           sbtPlugin := true
                         )) dependsOn(lib)*/

}
