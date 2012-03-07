import sbt._
import Keys._

object Build extends sbt.Build {
  //import coffeescript.Plugin._
  //import less.Plugin._
  //import ls.Plugin._
  //import heroic.Plugin._

  object Resolvers {
    val coda = "coda" at "http://repo.codahale.com"
  }

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "me.lessis",
    version := "0.1.1",
    publishMavenStyle := true,
    publishTo <<= version { (v: String) =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) 
        Some("snapshots" at nexus + "content/repositories/snapshots") 
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    homepage :=
      Some(new java.net.URL("http://ls.implicit.ly/")),
    publishArtifact in Test := false,
    licenses <<= (version)(v => Seq("MIT" -> url("https://github.com/softprops/tree/master/%s/LICENSE".format(v)))),
    pomExtra := (
      <scm>
        <url>git@github.com:softprops/ls.git</url>
        <connection>scm:git:git@github.com:softprops/ls.git</connection>
      </scm>
      <developers>
        <developer>
          <id>softprops</id>
          <name>Doug Tangren</name>
          <url>https://github.com/softprops</url>
        </developer>
      </developers>)
  )
  val dispatchVersion = "0.8.6"

  lazy val root = Project("root", file("."), settings = buildSettings ++ Seq(
    HeroShim.stage in Compile := {})) aggregate(
    plugin, lib, app
  ) 

  lazy val lib = Project("library", file("library"),
    settings = buildSettings ++ Seq(name := "ls", description := "ls library"))

  lazy val plugin = Project("plugin", file("plugin"),
    settings = buildSettings ++ Seq(
      sbtPlugin := true,
      name := "ls-sbt",
      description := "An sbt interface for ls.implicit.ly",
      libraryDependencies ++= Seq(
        "com.codahale" %% "jerkson" % "0.5.0",
        "net.databinder" %% "dispatch-http" % dispatchVersion
      ),
      resolvers += Resolvers.coda,
      publishMavenStyle := false,
      publishTo := Some(Resolver.url("sbt-plugin-releases", url(
        "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"
      ))(Resolver.ivyStylePatterns))
    ) ++ ScriptedPlugin.scriptedSettings /**++ lsSettings ++ Seq(      
      LsKeys.tags in LsKeys.lsync := Seq("ls", "plugin", "sbt"),
      externalResolvers in LsKeys.lsync := Seq(
        "coda" at "http://repo.codahale.com"
      ),
      LsKeys.docsUrl in LsKeys.lsync := Some(url("http://ls.implicit.ly/#publishing"))
    )*/) dependsOn(lib) /* here will not work in sbt as it generates pom with a sources classifier,
      references to class with in at that point result in class not found exceptions. sbt xsbt issue #257 */

  lazy val app = Project("app", file("app"),
    settings = buildSettings ++ 
      conscript.Harness.conscriptSettings ++ Seq(
        name := "ls-app",
        description := "ls project intializer",
        libraryDependencies += 
        "net.databinder" %% "dispatch-http" % dispatchVersion
      )
    ) dependsOn(lib)

}
