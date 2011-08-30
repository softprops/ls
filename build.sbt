import coffeescript._

organization := "me.lessis"

name := "ls"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.codahale" %% "jerkson" % "0.4.0",
  "net.databinder" %% "dispatch-http" % "0.8.3",
  "net.databinder" %% "unfiltered-filter" % "0.4.2-SNAPSHOT",
  "net.databinder" %% "unfiltered-jetty" % "0.4.2-SNAPSHOT",
  "net.databinder" %% "unfiltered-netty-server" % "0.4.2-SNAPSHOT",
  "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
)

resolvers += "coda" at "http://repo.codahale.com"

seq(CoffeeScript.coffeeSettings:_*)

targetDirectory in Coffee <<= (resourceManaged in Compile) { _ / "www" / "js" }
