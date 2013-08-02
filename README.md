# [ls.implicit.ly](http://ls.implicit.ly/)

a card calalog for scala libraries

For a quick overview, [see this screencast](http://www.screenr.com/EIus)

## install

### Via [conscript](https://github.com/n8han/conscript#readme)
    $ cs softprops/ls
    $ lsinit
    
### Via hands

If you are using sbt 0.12.* or 0.13.* add the following to your `project/plugins.sbt` file

    addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")

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
   



