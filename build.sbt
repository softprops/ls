organization := "me.lessis"

name := "ls"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.codahale" %% "jerkson" % "0.4.0",
  "net.databinder" %% "dispatch-http" % "0.8.3",
  "net.databinder" %% "unfiltered-filter" % "0.4.0",
  "net.databinder" %% "unfiltered-jetty" % "0.4.0"
)

resolvers += "coda" at "http://repo.codahale.com"
