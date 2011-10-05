package ls

import sbt._
import Keys._

object Plugin extends sbt.Plugin {
  import LsKeys._
  import java.io.File
  import java.net.URL

  case class Optionals(description: String, homepage: Option[URL], tags: Seq[String], docUrl: Option[URL])

  case class VersionInfo(
    org: String, name: String, version: String, opts: Optionals,
    resolvers: Seq[Resolver], libraryDeps: Seq[ModuleID]) {

    private def mjson(m: ModuleID) =
      """{
      | "organization":"%s",
      | "name": "%s",
      | "version": "%s"
      }""".stripMargin.format(
        m.organization, m.name, m.revision
      )

    lazy val json =
      """
      |{
      | "organization":"%s",
      | "name":"%s",
      | "version":"%s",
      | "description":"%s",
      | "site":"%s"
      | "tags":%s,
      | "docs":"%s",
      | "resolvers": %s,
      | "dependencies":{
      |   "libraries": %s
      | }
      |}""".stripMargin.format(
        org, name, version,
        opts.description,
        opts.homepage.getOrElse(""),
        opts.tags match {
          case Nil => "[]"
          case ts => ts.mkString("[\"","\",\"","\"]")
        },
        opts.docUrl.getOrElse(""),
        resolvers.mkString("""["""","""","""",""""]"""),
        libraryDeps.map(mjson).mkString("[",",","]")
      )
  }

  // fixme. this only serves to make the repl usage more user friendly
  val Ls = config("ls")

  object LsKeys {
    val versionInfo = SettingKey[VersionInfo]("version-info", "Information about a version of a project")
    val versionFile = SettingKey[File]("File storing version descriptor file")
    val writeVersion = TaskKey[Unit]("write-version", "Writes version data to descriptor file")
    val lsync = TaskKey[Unit]("lsync", "Synchronizes github project info with ls server")
    val dependencyFilter = SettingKey[ModuleID => Boolean]("dependency-filter",
                                                           "Filters library dependencies included in version-info")
    // option library info
    val tags = SettingKey[Seq[String]]("tags", "List of taxonomy tags for the library")
    val docsUrl = SettingKey[Option[URL]]("doc-url", "Url for library documentation")

    // private setting!
    val optionals = SettingKey[Optionals]("optionals", "Optional project info")
  }

  def lsSettings: Seq[Setting[_]] = inConfig(Ls)(Seq(
    version in Ls <<= (version in Runtime)(_.replace("-SNAPSHOT","")),
    sourceDirectory in Ls <<= (sourceDirectory in Compile)( _ / "ls"),
    versionFile <<= (sourceDirectory in Ls, version in Ls)(_ / "%s.json".format(_)),
    dependencyFilter := { m => m.organization != "org.scala-lang" },
    docsUrl := None,
    tags := Nil,
    description in Ls <<= (description in Runtime),
    homepage in Ls <<= (homepage in Runtime),
    optionals <<= (description in Ls, homepage in Ls, tags, docsUrl)((desc, homepage, tags, docs) =>
       Optionals(desc, homepage, tags, docs)
    ),
    versionInfo <<=
      (organization,
       name,
       version,
       optionals,
       resolvers,
       libraryDependencies,
       dependencyFilter) { (o, n, v, opts, rsvrs, ldeps, dfilter) =>
         VersionInfo(o, n, v, opts, rsvrs, ldeps.filter(dfilter))
       },
    writeVersion <<= (streams, versionFile, versionInfo) map {
      (out, f, info) =>
        if(!f.exists) {
          out.log.debug("Creating dirs for %s" format f)
          f.getParentFile().mkdirs()
          out.log.debug("writing %s to %s" format(info.json, f))
          IO.write(f, info.json)
        } else {
          val a = ask("version info for %s@%s already exists? To you wish to override it? [Y/N] " format(
            info.name, info.version
          )) {
            _.trim.toLowerCase
          }
          if(Seq("y", "yea", "yes", "yep") contains a) {
            out.log.debug("writing %s to %s" format(info.json, f))
            IO.write(f, info.json)
          } else if (Seq("n", "nah", "no","nope") contains a) {
            out.log.info("Canceling request")
          } else sys.error("Unexpected answer %s" format a)
        }
    }
  ))

  private def ask[T](q: String)(f: String => T): T = q.synchronized {
    print(q)
    f(scala.Console.readLine)
  }
}
