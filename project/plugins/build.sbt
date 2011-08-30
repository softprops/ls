resolvers += "lessis" at "http://repo.lessis.me"

libraryDependencies <+= sbtVersion(v => "me.lessis" %% "coffeescripted-sbt" % "0.1.4-%s-SNAPSHOT".format(v))
