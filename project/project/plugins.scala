import sbt._
object PluginDef extends Build {
  lazy val root = Project("plugins", file(".")) dependsOn( heroic )
  lazy val heroic = file("../lib/heroic")
}
