import sbt._

class Project(info: ProjectInfo) extends AppengineProject(info)
  with DataNucleus {

  val uf_version = "0.3.1"
  lazy val uff = "net.databinder" %% "unfiltered-filter" % uf_version
  lazy val ufj = "net.databinder" %% "unfiltered-jetty" % uf_version
  lazy val ufjs = "net.databinder" %% "unfiltered-json" % uf_version

  val dispatch_version = "0.8.0.Beta3"
  lazy val dhttp = "net.databinder" %% "dispatch-http-gae" % dispatch_version
  lazy val djson = "net.databinder" %% "dispatch-lift-json" % dispatch_version

  // persistence
  val appengineRepo = "nexus" at "http://maven-gae-plugin.googlecode.com/svn/repository/"
  val jdo = "javax.jdo" % "jdo2-api" % "2.3-ea"

  // testing
  lazy val uf_spec = "net.databinder" %% "unfiltered-spec" % uf_version % "test"
  lazy val jboss = "JBoss repository" at
    "https://repository.jboss.org/nexus/content/groups/public/"
}
