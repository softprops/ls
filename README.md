# [ls.implicit.ly](http://ls.implicit.ly/)

a card calalog for scala libraries

For a quick overview, [see this screencast](http://www.screenr.com/EIus)

## install

### Via [conscript](https://github.com/n8han/conscript#readme)
    $ cs softprops/ls
    $ lsinit
    
### Via hands

Edit your `project/plugins.sbt` file

    addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")
   
Add resolvers for sbt 0.11.2

    resolvers ++= Seq(
      "less is" at "http://repo.lessis.me",
      "coda" at "http://repo.codahale.com"
    )
    
Or if you are using sbt 0.11.3, use the sbt community plugin resolver

    resolvers ++= Seq(
      Resolver.url("sbt-plugin-releases", new URL(
        "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(
          Resolver.ivyStylePatterns),
      "coda" at "http://repo.codahale.com"
    )

There is also a release availble if you are using sbt 0.12.0-RC1

   addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")

   resolvers ++= Seq(
     Classpaths.sbtPluginReleases,
     Opts.resolver.sonatypeReleases
   )

Mix it in to your project definition

    seq(lsSettings: _*)

For more information. See the [site](http://ls.implicit.ly/#publishing)   

## Usage

See the [site](http://ls.implicit.ly/#publishing)

## Issues

[blame me](https://github.com/softprops/ls/issues)


Doug Tangren (softprops) 2011
   



