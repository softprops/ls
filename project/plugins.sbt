resolvers ++= Seq(
  "lessis" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com"
)

addSbtPlugin("me.lessis" % "coffeescripted-sbt" % "0.2.0")

addSbtPlugin("me.lessis" % "less-sbt" % "0.1.3")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.0")

//addSbtPlugin("me.lessis" % "heroic" % "0.1.0-SNAPSHOT")

addSbtPlugin("net.databinder" % "conscript-plugin" % "0.3.2")
