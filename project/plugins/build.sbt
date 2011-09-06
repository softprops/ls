resolvers += "lessis" at "http://repo.lessis.me"

libraryDependencies <+= sbtVersion(v => "me.lessis" %% "coffeescripted-sbt" % "0.1.5-%s".format(v))
