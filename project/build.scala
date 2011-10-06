import sbt._
import Keys._

object Build extends sbt.Build {
  import coffeescript.CoffeeScript
  import CoffeeScript._
  import less.Plugin._

  lazy val svr = Project("server", file("server"),
                         settings = Defaults.defaultSettings ++ Seq(
                           organization := "me.lessis",
                           name := "ls",
                           version := "0.1.0-SNAPSHOT",
                           libraryDependencies ++= Seq(
                             "com.codahale" %% "jerkson" % "0.4.2",
                             "net.databinder" %% "dispatch-http" % "0.8.5",
                             "net.databinder" %% "unfiltered-netty-server" % "0.5.0",
                             "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
                           )) ++ CoffeeScript.coffeeSettings ++ lessSettings ++ Seq(
                           (targetDirectory in Coffee) <<= (resourceManaged in Compile) { _ / "www" / "js" },
                           (resourceManaged in (Compile, LessKeys.less)) <<= (resourceManaged in Compile) { _ / "www" / "css" },
                           (LessKeys.mini in (Compile, LessKeys.less)) := true,
                           resolvers += "coda" at "http://repo.codahale.com"
                         ))

  lazy val plugin = Project("plugin", file("plugin"),
                          settings = Defaults.defaultSettings ++ Seq(
                            sbtPlugin := true,
                            organization := "me.lessis",
                            name := "ls-sbt",
                            version := "0.1.0-SNAPSHOT",
                            libraryDependencies +=
                              "net.databinder" %% "dispatch-http" % "0.8.5"
                          ))

}
