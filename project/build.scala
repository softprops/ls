import sbt._
import Keys._

object Build extends sbt.Build {
  import ls.Plugin._

  val buildSettings = Defaults.defaultSettings ++ Seq(
    scalacOptions ++= Seq(Opts.compile.deprecation, "-feature"),
    organization := "me.lessis",
    version := "0.1.3",
    sbtVersion in Global := "0.13.0-RC4",
    scalaVersion in Global := "2.10.2",
    publishArtifact in Test := false,
    publishMavenStyle := true,
    publishTo := Some(Opts.resolver.sonatypeStaging),
    licenses <<= version(v =>
      Seq("MIT" ->
          url("https://github.com/softprops/ls/blob/%s/LICENSE" format v))),
    homepage := some(url("http://ls.implicit.ly/")),
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

  lazy val root = Project(
    "root", file("."),
    settings = buildSettings ++ Seq(
      test := { }, // no tests
      publish := { }, // skip publishing for this root project.
      publishLocal := { }, // skip publishing locally,
      ls.Plugin.LsKeys.skipWrite := true // don't track root in ls
    )
  ) aggregate(plugin, lib, app)

  lazy val lib = Project(
    "library",
    file("library"),
    settings = buildSettings ++ Seq(
    name := "ls",
    libraryDependencies ++= Seq(
      "net.databinder.dispatch" %% "dispatch-json4s-native" % "0.11.0",
      "org.slf4j" % "slf4j-jdk14" % "1.6.2")
    )
  )

  lazy val plugin = Project("plugin", file("plugin"),
    settings = buildSettings ++ Seq(
      sbtPlugin := true,
      name := "ls-sbt",
      libraryDependencies ++= Seq(
        "me.lessis" %% "pj" % "0.1.0"
      ),
      resolvers ++= Seq(Opts.resolver.sonatypeReleases),
      publishTo := Some(Classpaths.sbtPluginReleases),
      publishMavenStyle := false,
      description := "An sbt interface for ls.implicit.ly"
    ) ++ ScriptedPlugin.scriptedSettings ++ lsSettings ++ Seq(
      LsKeys.tags in LsKeys.lsync := Seq("ls", "plugin", "sbt"),
      LsKeys.docsUrl in LsKeys.lsync := Some(url("http://ls.implicit.ly/#publishing"))
    )) dependsOn(lib)

  lazy val app = Project("app", file("app"),
    settings = buildSettings ++ Seq(
      resolvers += Classpaths.typesafeResolver,
      libraryDependencies <+= (sbtVersion)(
        "org.scala-sbt" %
        "launcher-interface" %
        _ % "provided"),
      name := "ls-app"
    )
  ) dependsOn(lib)
}
