addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")

libraryDependencies <+= sbtVersion("org.scala-sbt" % "scripted-plugin" % _)

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")
