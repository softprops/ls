organization := "me.lessis"

name := "ls"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.codahale" %% "jerkson" % "0.4.0",
  "net.databinder" %% "dispatch-http" % "0.8.3",
  "net.databinder" %% "unfiltered-netty" % "0.4.0",
  "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
)

resolvers += "coda" at "http://repo.codahale.com"
