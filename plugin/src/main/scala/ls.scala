package ls

import sbt._
import Keys._
import Project.Initialize
import scala.language.postfixOps
import scala.util.control.NonFatal

object Plugin extends sbt.Plugin
  with Requesting with LiftJsonParsing {
  import scala.collection.JavaConversions._
  
  import LsKeys.{ ls => lskey, _ }
  import java.io.File
  import java.net.URL
  import dispatch._
  import sbt.{ ModuleID => SbtModuleID }
  import ls.{ LibraryVersions, Library }

  object LsKeys {
    // general
    val colors = SettingKey[Boolean](key("colors"), "Colorize logging")
    val host = SettingKey[String](key("host"), "Host ls server")
    val usage = TaskKey[Unit]("usage", "Displays usage information for tasks")

    // github
    val ghUser = SettingKey[Option[String]](key("gh-user"), "Github user name")
    val ghRepo = SettingKey[Option[String]](key("gh-repo"), "Github repository name")
    val ghBranch = SettingKey[Option[String]](key("gh-branch"), "Github branch name")

    // syncing
    val lint        = TaskKey[Boolean](key("lint"), "Verifies the structure of serialized version info")
    val cat         = TaskKey[Unit](key("cat"),
                                    "Prints the contents of the current serialized version file to the console")
    val versionInfo = TaskKey[VersionInfo](key("version-info"), "Information about a version of a project")
    val versionFile = SettingKey[File](key("version-file"), "File storing version descriptor file")
    val writeVersion = TaskKey[Unit](key("write-version"), "Writes version data to descriptor file")
    val lsync = TaskKey[Unit]("lsync", "Synchronizes github project info with ls server")
    val dependencyFilter = SettingKey[SbtModuleID => Boolean]("dependency-filter",
      "Filters library dependencies included in version-info")
    // optional attributes
    val tags = SettingKey[Seq[String]](key("tags"), "List of taxonomy tags for the library")
    val docsUrl = SettingKey[Option[URL]](key("doc-url"), "Url for library documentation")
    val optionals = SettingKey[Optionals](key("optionals"), "Optional project info")
    val skipWrite = SettingKey[Boolean](key("skip-write"), "Skip this module in write-version")

    // discovery
    val ls = InputKey[Unit]("ls", "Search for remote libraries")
    val lsDocs = InputKey[Unit](key("docs"), "Launch library documentation")

    private def key(name: String) = "ls-%s" format name
  }

  val DefaultLsHost = "http://ls.implicit.ly"

  private def catTask: Def.Initialize[Task[Unit]] =
    (streams, versionFile, versionInfo) map {
      (out, vfile, vinfo) =>
        if (vfile.exists) {
          out.log.info("version %s @ %s" format(vinfo.name, vinfo.version))
          println(IO.read(vfile))
        } else {
          out.log.warn(
            "Version %s @ %s did not exist. Create one with `ls-write`" format(
              vinfo.name, vinfo.version))
        }
    }

  private def lintTask: Def.Initialize[Task[Boolean]] =
    (streams, versionFile, versionInfo) map {
      (out, vfile, vinfo) => try {
        if(vfile.exists) {
          val json = IO.read(vfile)
          // todo: parse with json4s
          parseJson[Library](json)
        }
        out.log.debug("Valid version info")
        if (snapshot(vinfo.version)) out.log.warn(SnapshotWarning)
        true
      } catch {
        case NonFatal(e) =>
          out.log.error("Invalid version-info %s. %s" format(
            e.getMessage, if(vfile.exists) "\n" + IO.read(vfile) else ""
          ))
          false
      }
    }

  private def lsyncTask: Def.Initialize[Task[Unit]] =
    (streams, ghUser in lsync, ghRepo in lsync, ghBranch in lsync, version in lsync,
     host in lsync, versionFile, lint) map {
      (out, maybeUser, maybeRepo, branch, vers, host, vfile, lint) =>
        (maybeUser, maybeRepo) match {
          case (Some(user), Some(repo)) =>
            if (lint) {
              if (snapshot(vers)) {
                out.log.warn(SnapshotWarning)
              } else {
                out.log.info("lsyncing project %s/%s@%s..." format(user, repo, vers))
                try {
                  // todo: should this by an async server request?
                  http(Client(host).lsync(user, repo, branch.getOrElse("master"), vers) as_str)
                  out.log.info("Project was synchronized")
                } catch {
                  case NonFatal(e) =>
                    out.log.warn("Error synchronizing project libraries %s" format e.getMessage)
                }
              }
            } else sys.error("Your version descriptor was invalid. %s" format(
              IO.read(vfile)
            ))
          case _ => sys.error("Could not resolve a Github git remote")
        }
    }

  private def snapshot(vstr: String) = vstr.toUpperCase.endsWith("SNAPSHOT")

  private val SnapshotWarning = "ls only supports release versions not likely to change. This excludes snapshot versions."

  private def writeVersionTask: Def.Initialize[Task[Unit]] = 
     (streams, versionFile, versionInfo, skipWrite) map {
      (out, f, info, skip) =>
        def write() {
          out.log.debug("version info: %s" format(info.json))
          IO.write(f, info.json)
          out.log.info("Wrote %s" format(f))
        }
        if (skip) {
          out.log.info("Skipping %s".format(f))
        } else if(!f.exists) {
          f.getParentFile().mkdirs()
          write()
        } else Prompt.ask(
          "Overwrite existing version info for %s@%s? [Y/n] " format(
            info.name, info.version
          )) { r =>
            val a = r.trim.toLowerCase
            if(Prompt.Yes.contains(a) || a.trim.isEmpty) {
              write()
            }
            else if(Prompt.No contains a) out.log.info("Skipped.")
            else sys.error("Unexpected answer %s" format a)
          }
    }

  object LibraryParser {
    trait LibraryParserResult
    case object Fail extends LibraryParserResult
    case class Pass(user: Option[String], repo: Option[String],
                    library: String, version: Option[String],
                    config: Option[String]) extends LibraryParserResult

    val Pat = """^((\S+)/(\S+)/)?(\S+(?=@)|\S+(?!@))(?:@(\S+(?=[:])|\S+(?![:])))?(?:[:](\S+))?$""".r

    /**
     * format specification
     *
     * [user/repository/]library[@version][:config1->config2]
     *
     * library is *required
     * version is optional
     * :config is optional will translate to an ivy config
     * user/repository/ is optional and namespaces the library (the common case
     *  being that most people will use the same name for their libraries
     * )
     */
    def apply(raw: String): LibraryParserResult = raw match {
      case Pat(_, user, repo, library, version, config) =>
        Pass(Option(user), Option(repo),
             library, Option(version), Option(config))
      case undefined => Fail
    }
  }

  /**
   * Shortcut for adding abbreviated library dependencies.
   * use ls-try for testing out transient library dependencies
   * or  ls-install to persist the library depenency to your project
   * configuration
   * 
   * examples:
   *   ls-try unfiltered/unfiltered/unfiltered-netty@0.4.2
   *   ls-try unfiltered-netty
   */
  private def depend(persistently: Boolean)(state: State, dep: String) =
    LibraryParser(dep) match {
      case LibraryParser.Pass(user, repo, lib, version, config) =>
        try {
          val resp = http(Client(DefaultLsHost).lib(lib, version)(user)(repo) as_str)
          // todo: parse with json4s
          val ls = parseJson[List[LibraryVersions]](resp)
          Depends(state, ls, version, config, persistently)
        } catch {
          case dispatch.StatusCode(404, msg) => sys.error(
            "Library not found %s" format msg
          )
          case Conflicts.Conflict(_, _, _, msg) => sys.error(
            msg
          )
        }
      case LibraryParser.Fail => sys.error(
        "Unexpected library format %s. Try something like %s" format(
          dep, "[user/repository/]library[@version][:config1->config2]"
        )
      )
    }

  def lsTry = Command.single("ls-try")(depend(false))

  def lsInstall = Command.single("ls-install")(depend(true))

  def lsSearchSettings: Seq[Setting[_]] = Seq(
    host in lskey := DefaultLsHost,
    colors in lskey := true,
    usage in lskey <<= (streams) map {
      (out) =>
        out.log.info("""
        |Usage: ls [-n] [terms...]
        |  
        |Examples
        |  # find a library named unfiltered
        |  ls -n unfiltered
        |  # find a library named unfiltered at version of 0.5.1
        |  ls -n unfiltered@0.5.1
        |  # find libraries taged with terms web or http
        |  ls web http
        """.stripMargin)
    },
    lsDocs <<= inputTask { (argsTask: TaskKey[Seq[String]]) =>
      (argsTask, streams, host in lskey, colors in lskey, state) map {
        case (args, out, host, color, state) =>
          val log = out.log
          args match {
            case Seq(name) =>
              val cli = Client(host)
              def named(name: String) = name.split("@") match {
                case Array(name) => cli.lib(name)_
                case Array(name, version) => cli.lib(name, version = Some(version))_
              }
              try {
                log.info("Fetching library docs for %s" format name)
                def version(name: String) = name.split("@") match {
                  case Array(_) => None
                  case Array(_, version) => Some(version)
                }
                val resp = http(named(name)(None)(None) as_str)
                docsFor(
                  // todo: parse with json4s
                  parseJson[List[LibraryVersions]](resp),
                  version(name), log)
              } catch {
                case StatusCode(404, msg) =>
                  log.info("`%s` library not found" format name)
              }
            case _ => sys.error(
              "Please provide a name and optionally a version of the library you want docs for in the form ls-docs <name> or ls-docs <name>@<version>"
            )
          }
        }
    },
    lskey <<= inputTask { (argsTask: TaskKey[Seq[String]]) =>
      (argsTask, streams, host in lskey, colors in lskey, state) map {
        case (args, out, host, color, state) =>
          val log = out.log
          args match {
            case Seq() => sys.error(
              "Please provide at least one or more search keywords or -n <name of library>"
            )
            case Seq("-n", name) =>
              val cli = Client(host)
              def named(name: String) = name.split("@") match {
                case Array(name) => cli.lib(name)_
                case Array(name, version) => cli.lib(name, version = Some(version))_
              }
              try {
                log.info("Fetching library info for %s" format name)
                val resp = http(named(name)(None)(None) as_str)
                libraries(
                  // todo: parse json4s
                  parseJson[List[LibraryVersions]](resp),
                  log, color, -1,
                  name.split("@"):_*)
              } catch {
                case StatusCode(404, msg) =>
                  out.log.info("`%s` library not found" format name)
              }
            case kwords =>
              val cli = Client(host)
              try {
                log.info("Fetching library info matching %s" format kwords.mkString(", "))
                val resp = http(cli.search(kwords.toSeq) as_str)
                libraries(
                  // todo: parse json4s
                  parseJson[List[LibraryVersions]](resp),
                  log, color, 3,
                  args:_*
                )
              } catch {
                case StatusCode(404, msg) =>
                  log.info("Library not found for keywords %s" format kwords.mkString(", "))
              }
          }
      }
    },
    (aggregate in lskey) := false,
    (aggregate in lsDocs) := false,
    commands ++= Seq(lsTry, lsInstall)
  )

  /** Assume the default resolver is the sonotype oss repo */
  lazy val DefaultResolvers = Seq(Opts.resolver.sonatypeReleases)

  def lsPublishSettings: Seq[Setting[_]] = Seq(
    host in lsync := DefaultLsHost,
    colors in lsync := true,
    version in lsync <<= (version in Runtime)(_.replace("-SNAPSHOT","")),
    sourceDirectory in lsync <<= (sourceDirectory in Compile)( _ / "ls"),
    versionFile <<= (sourceDirectory in lsync, version in lsync)(_ / "%s.json".format(_)),
    // exclude scala lib and test dependencies by default
    dependencyFilter := { m =>
      !(scalaLib(m) || testDependency(m))
    },
    docsUrl in lsync := None,
    tags in lsync := Nil,
    description in lsync <<= (description in Runtime),
    homepage in lsync <<= (homepage in Runtime),
    optionals <<= (description in lsync, homepage in lsync, tags in lsync,
                   docsUrl in lsync, licenses in lsync)(
      (desc, homepage, tags, docs, lics) =>
        Optionals(desc, homepage, tags, docs, lics.map {
          case (name, url) => License(name, url.toString)
        }
      )
    ),
    skipWrite := false,
    externalResolvers in lsync := DefaultResolvers,
    licenses in lsync <<= licenses in Runtime,
    versionInfo <<=
      (organization,
       name,
       version,
       optionals,
       externalResolvers in lsync,
       libraryDependencies,
       dependencyFilter,
       sbtPlugin,
       crossScalaVersions) map {
        (o, n, v, opts, rsvrs, ldeps, dfilter, csv, pi) =>
          VersionInfo(o, n, v, opts, rsvrs, ldeps.filter(dfilter), pi, csv)
       },
    lint <<= lintTask,
    cat <<= catTask,
    writeVersion <<= writeVersionTask,
    ghUser in lsync := (Git.ghRepo match {
      case Some((user, _)) => Some(user)
      case _ => None
    }),
    ghRepo in lsync := (Git.ghRepo match {
      case Some((_, repo)) => Some(repo)
      case _ => None
    }),
    ghBranch in lsync := Git.branch.orElse(Some("master")),
    lsync <<= lsyncTask,
    (aggregate in lsync) := false
  )

  def lsSettings: Seq[Setting[_]] = lsSearchSettings ++ lsPublishSettings

  /** Preforms a `best-attempt` at retrieving a uri for library documentation before
   *  attempting to launch it */
  private def docsFor(libs: Seq[LibraryVersions], targetVersion: Option[String], log: sbt.Logger) =
    libs match {
      case Seq() => log.info("Library not found")
      case Seq(lib) =>
        (targetVersion match {
          case Some(v) => lib.versions.find(_.version == v)
          case _ => lib.versions.headOption
        }) match {
          case Some(vers) =>
            (vers.docs match {
              case s if(!s.isEmpty) => Some(s)
              case _ => lib.site match {
                case s if(!s.isEmpty) => Some(s)
                case _ => ((lib.ghuser, lib.ghrepo)) match {
                  case (Some(u), Some(r)) => Some("https://github.com/%s/%s/" format(
                    u, r
                  ))
                  case _ => None
                }
              }
            }) match {
              case Some(d) => launch(d) match {
                case Some(err) => log.warn("Unable to launch docs %s" format d)
                case _ => ()
              }
              case _ => log.info("No docs available for %s@%s" format(
                lib.name, vers.version
              ))
            }
          case _ => log.info("No docs available for %s" format lib.name)
        }
      case _ => log.info("More than one library found, try to narrow your search")
    }

  /** Attempts to launch the provided uri */
  private def launch(uri: String) =
    uri match {
      case u if(!u.isEmpty) =>
        try {
          import java.net.URI
          val dsk = Class.forName("java.awt.Desktop")
          dsk.getMethod("browse", classOf[URI]).invoke(
            dsk.getMethod("getDesktop").invoke(null), new URI(u)
          )
          None
        } catch { case NonFatal(e) => Some(e) }
      case empty => None
    }


  private def libraries(libs: Seq[LibraryVersions], log: sbt.Logger, colors: Boolean, maxVersions: Int, terms: String*) = {
    def versions(l: LibraryVersions) =
      if (maxVersions == -1 || l.versions.size < maxVersions) l.versions.map(_.version)
      else l.versions.take(maxVersions).map(_.version) :+ "..."

    val tups = libs.map(l => (l.name, versions(l).mkString(", "), l.description))
    val len = math.max(tups.map{ case (n, vs, _ ) => "%s (%s)".format(n, vs).size }.sortWith(_>_).head, 10)
    val fmt = "%s %-" + len + "s # %s"
    val lterms: Seq[String] = terms.toList
    def hl(txt: String, terms: Seq[String],
           cw: Wheel[String] = Wheels.default): String =
      if(colors) {
        terms match {
          case head :: Nil =>
            txt.replaceAll("""(?i)(\?\S+)?(""" + head + """)(\?\S+)?""", cw.get + "$0\033[0m").trim
          case head :: tail =>
            hl(txt.replaceAll("""(?i)(\?\S+)?(""" + head + """)(\\S+)?""", cw.get + "$0\033[0m").trim,
               tail, cw.turn)
          case Nil => txt
        }
      } else txt

    val clrs = Wheels.shuffle
    tups map { case (n, v, d) =>
      println(
        hl(fmt format(
          " -",
          "%s (%s)".format(n,v),
          d
        ), lterms, Wheels.colorWheel(clrs))
      )
    }
    if(libs.isEmpty) log.info("(no projects matching the terms %s)" format terms.mkString(" "))
  }

  /* https://github.com/harrah/xsbt/wiki/Configurations */
  private def testDependency(m: sbt.ModuleID) =
    m.configurations match {
      case Some(conf) => conf.trim.toLowerCase.startsWith("test")
      case _ => false
    }

  private def scalaLib(m: sbt.ModuleID) =
    m.organization.trim.toLowerCase.equals(
      "org.scala-lang"
    )

}
