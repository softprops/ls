package ls

import sbt._
import Keys._
import Project.Initialize

object Plugin extends sbt.Plugin {
  import LsKeys._
  import java.io.File
  import java.net.URL
  import dispatch._

  case class Optionals(description: String, homepage: Option[URL], tags: Seq[String], docUrl: Option[URL])

  case class VersionInfo(
    org: String, name: String, version: String, opts: Optionals,
    resolvers: Seq[Resolver], libraryDeps: Seq[ModuleID],
    scalaVersions: Seq[String], sbt: Boolean) {

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
      | "site":"%s",
      | "tags":%s,
      | "docs":"%s",
      | "resolvers": %s,
      | "dependencies":{
      |   "libraries": %s
      | },
      | "scala_versions": %s,
      | "sbt": %s
      |}""".stripMargin.format(
        org, name, version,
        opts.description,
        opts.homepage.getOrElse(""),
        opts.tags match {
          case Nil => "[]"
          case ts => ts.mkString("""["""","""","""",""""]""")
        },
        opts.docUrl.getOrElse(""),
        resolvers.mkString("""["""","""","""",""""]"""),
        libraryDeps.map(mjson).mkString("[",",","]"),
        scalaVersions.mkString("""["""","""","""",""""]"""),
        sbt
      )
  }

  // fixme. this only serves to make the repl usage more user friendly
  val Ls = config("ls")

  object LsKeys {
    val colors = SettingKey[Boolean]("colors", "Colorize logging")
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

    val ghUser = SettingKey[Option[String]]("gh-user", "Github user name")
    val ghRepo = SettingKey[Option[String]]("gh-repo", "Github repository name")

    val lsHost = SettingKey[String]("ls-host", "Host ls server")
    val find = InputKey[Unit]("find",
                              "Search for projects based on <user>, <user> <repo>, or <user> <repo> <version>")
  }

  private def http[T](hand: Handler[T]): T = {
    val h = new Http
    try { h(hand) }
    finally { h.shutdown() }
  }

  private def lsyncTask: Initialize[Task[Unit]] =
    (streams, ghUser, ghRepo, version in Ls, lsHost) map {
      (out, maybeUser, maybeRepo, vers, host) =>
        (maybeUser, maybeRepo) match {
          case (Some(user), Some(repo)) =>
            http((dispatch.url(host).POST / "api" / "libraries") << Map(
              "user" -> user,
              "repo" -> repo,
              "version" -> vers
            ) as_str)
          case _ => sys.error("Could not resolve a github git remote")
        }
    }

  private def writeVersionTask: Initialize[Task[Unit]] = 
     (streams, versionFile, versionInfo) map {
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

  def lsSettings: Seq[Setting[_]] = inConfig(Ls)(Seq(
    colors := true,
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
       dependencyFilter,
       sbtPlugin,
       crossScalaVersions) { (o, n, v, opts, rsvrs, ldeps, dfilter, csv, pi) =>
         VersionInfo(o, n, v, opts, rsvrs, ldeps.filter(dfilter), pi, csv)
       },
    writeVersion <<= writeVersionTask,
    ghUser := (maybeRepo match {
      case Some((user, _)) => Some(user)
      case _ => None
    }),
    ghRepo := (maybeRepo match {
      case Some((_, repo)) => Some(repo)
      case _ => None
    }),
    lsHost := "http://localhost:5000",
    lsync <<= lsyncTask,
    find <<= inputTask { (argsTask: TaskKey[Seq[String]]) =>
      (argsTask, streams, lsHost) map {
        case (args, out, host) =>
          val cli = Client(host)
          // todo: query dsl
          args match {
            case Seq(user) =>
              out.log.info(
                http(cli.user(user) as_str)
              )
            case Seq(user, repo) =>
              out.log.info(
                http(cli.repository(user, repo) as_str)
              )
            case Seq(user, repo, version) =>
              out.log.info(
                http(cli.version(user, repo, version) as_str)
              )
          }
      }
    },
    (aggregate in find) := false
  ))

  object GhRepo {
    val GHRemote = """^git@github.com[:](\S+)/(\S+)[.]git$""".r
     def unapply(line: String) = line.split("""\s+""") match {
       case Array(_, GHRemote(user, repo), _) => Some(user, repo)
       case a => println(a.toList);None
     } 
   }

  def maybeRepo: Option[(String, String)] = {
    (Process("git remote -v") !!).split("""\n""").collectFirst {
      case GhRepo(user, repo) => (user, repo)
    }
  }

  private def ask[T](q: String)(f: String => T): T = q.synchronized {
    print(q)
    f(scala.Console.readLine)
  }
}
