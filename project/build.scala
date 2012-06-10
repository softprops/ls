import sbt._
import Keys._

object Build extends sbt.Build {
  //import ls.Plugin._

  object Resolvers {
    val coda = "coda" at "http://repo.codahale.com"
  }

  val buildSettings = Seq(
    scalacOptions += Opts.compile.deprecation,
    organization <<= organization ?? "me.lessis",
    version <<= (version, version in GlobalScope){ (v,vg) =>
      if (v == vg) "0.1.2-SNAPSHOT" else v
    },
    publishArtifact in Test := false,
    publishMavenStyle := true,
    publishTo := Some(Opts.resolver.sonatypeReleases),
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
  ) ++ Defaults.defaultSettings

  val dispatchVersion = "0.8.8"

  lazy val root = Project("root", file("."), settings = buildSettings) aggregate(
    plugin, lib/*, app*/
  ) 

  lazy val lib = Project("library", file("library"),
    settings = buildSettings ++ Seq(
      libraryDependencies +=
        "net.databinder" %% "dispatch-http" % dispatchVersion,
      name := "ls"
      )
    )

  lazy val plugin = Project("plugin", file("plugin"),
    settings = buildSettings ++ Seq(
      sbtPlugin := true,
      name := "ls-sbt",
      libraryDependencies ++= Seq(
        "com.codahale" % "jerkson_2.9.1" % "0.5.0",
        "me.lessis" %% "pj" % "0.1.0" exclude(
          "org.codehaus.jackson", "jackson-core-asl"),
        "me.lessis" %% "ls" % "0.1.2-SNAPHOT"
      ),
      resolvers += Resolvers.coda,
      publishTo := Some(Classpaths.sbtPluginReleases),
      publishMavenStyle := false
    ) ++ ScriptedPlugin.scriptedSettings /* ++ lsSettings ++ Seq(
      description in LsKeys.lsync := "An sbt interface for ls.implicit.ly",
      LsKeys.tags in LsKeys.lsync := Seq("ls", "plugin", "sbt"),
      externalResolvers in LsKeys.lsync := Seq(
        "less is" at "http://repo.lessis.me",
        "coda" at "http://repo.codahale.com"
      ),
      LsKeys.docsUrl in LsKeys.lsync := Some(url("http://ls.implicit.ly/#publishing")),
    )*/)// dependsOn(lib)

  lazy val app = Project("app", file("app"),
    settings = buildSettings ++ 
      /*conscript.Harness.conscriptSettings ++ */Seq(
         // don't publish until sbt 0.12.0 is final to avoid
        // punishing 0.11 users
        publishTo := None,
        resolvers += Classpaths.typesafeResolver,
        libraryDependencies <+= (sbtVersion)(
          "org.scala-sbt" %
          "launcher-interface" %
          _ % "provided"),
        name := "ls-app"
      )
    ) dependsOn(lib)
}
