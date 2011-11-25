# [ls.implicit.ly](http://ls.implicit.ly/)

a card calalog for scala libraries

## install

### Via [conscript](https://github.com/softprops/ls/blob/master/notes/0.1.0.markdown)
    $ cs softprops/ls
    $ lsinit
    
### Via hands

Edit your `project/plugins.sbt` file

    addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.0")
   
    resolvers ++= Seq(
      "less is" at "http://repo.lessis.me",
      "coda" at "http://repo.codahale.com"
    )
    
Mix it in to your project definition 

    seq(lsSettings: _*)

For more information. See the [site](http://ls.implicit.ly/#publishing)   

## Usage

See the [site](http://ls.implicit.ly/#publishing)

## Issues

[blame me](http://ls.implicit.ly/#publishing)


Doug Tangren (softprops) 2011
   



