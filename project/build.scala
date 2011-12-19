import sbt._
import Keys._

object Build extends sbt.Build {
  import ls.Plugin._

  object Resolvers {
    val coda = "coda" at "http://repo.codahale.com"
  }

  val buildSettings = Seq(
    organization <<= organization ?? "me.lessis",
    version <<= version ?? "0.1.2-SNAPSHOT",
    publishTo <<= publishTo ??
      Some(Resolver.file("lessis repo", new java.io.File("/var/www/repo")))
  ) ++ Defaults.defaultSettings

  val dispatchVersion = "0.8.6"

  lazy val root = Project("root", file("."), settings = buildSettings) aggregate(
    plugin, lib, app
  ) 

  lazy val lib = Project("library", file("library"),
    settings = buildSettings ++ Seq(
      libraryDependencies +=
        "net.databinder" %% "dispatch-http" % dispatchVersion,
      name := "ls")
    )

  lazy val plugin = Project("plugin", file("plugin"),
    settings = buildSettings ++ Seq(
      sbtPlugin := true,
      name := "ls-sbt",
      libraryDependencies ++= Seq(
        "com.codahale" %% "jerkson" % "0.5.0",
        "me.lessis" %% "ls" % "0.1.1"
      ),
      resolvers += Resolvers.coda
    ) ++ ScriptedPlugin.scriptedSettings /* ++ lsSettings ++ Seq(
      description in LsKeys.lsync := "An sbt interface for ls.implicit.ly",
      LsKeys.tags in LsKeys.lsync := Seq("ls", "plugin", "sbt"),
      externalResolvers in LsKeys.lsync := Seq(
        "less is" at "http://repo.lessis.me",
        "coda" at "http://repo.codahale.com"
      ),
      LsKeys.docsUrl in LsKeys.lsync := Some(url("http://ls.implicit.ly/#publishing")),
      homepage in LsKeys.lsync := Some(url("http://ls.implicit.ly/")),
      licenses in LsKeys.lsync := Seq(
        ("MIT", url("https://github.com/softprops/ls/blob/master/LICENSE"))
      )
    )*/) dependsOn(lib) /* here will not work in sbt as it generates pom with a sources classifier,
      references to class with in at that point result in class not found exceptions. sbt xsbt issue #257 */

  lazy val app = Project("app", file("app"),
    settings = buildSettings ++ 
      conscript.Harness.conscriptSettings ++ Seq(
        name := "ls-app"
      )
    ) dependsOn(lib)
}
