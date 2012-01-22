package ls

import sbt._
import Keys._
import Project.Initialize

trait Requesting {
  import dispatch._
  import org.apache.http.conn.{ HttpHostConnectException => ConnectionRefused }

  def http[T](hand: Handler[T]): T = {
    val h = new Http with NoLogging
    try { h(hand) }
    catch {
      case cf: ConnectionRefused => sys.error(
        "ls is currently not available to take your call"
      )
      case uh:java.net.UnknownHostException => sys.error(
        "You may not know your host as well as you think. Your http client doesn't know %s" format uh.getMessage
      )
    }
    finally { h.shutdown() }
  }
}

object Plugin extends sbt.Plugin with Requesting {
  import ls.ClassLoaders._
  import scala.collection.JavaConversions._
  import com.codahale.jerkson.Json._
  import com.codahale.jerkson.ParsingException
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

  private def lintTask: Initialize[Task[Boolean]] =
    (streams, versionFile, versionInfo) map {
      (out, vfile, vinfo) => try {
        // note: calling #json on version info triggers lazy jerkson serialization
        vinfo.json
        if(vfile.exists) {
          val json = IO.read(vfile)
          inClassLoader(classOf[Library]) {
            parse[Library](json)
          }
        }
        out.log.debug("Valid version info")
        true
      } catch {
        case e: ClassNotFoundException =>
          out.log.error("A class loading error was thrown for %s. This is a known issue with the jerkson json library and sbt. Please post an issue at http://github.com/softprops/ls/issues"
                        .format(e.getMessage))
           false
        case e =>
          e.printStackTrace
          out.log.error("Invalid version-info %s. %s" format(
            e.getMessage, if(vfile.exists) "\n" + IO.read(vfile) else ""
          ))
          false
      }
    }

  private def lsyncTask: Initialize[Task[Unit]] =
    (streams, ghUser in lsync, ghRepo in lsync, ghBranch in lsync, version in lsync,
     host in lsync, versionFile, lint) map {
      (out, maybeUser, maybeRepo, branch, vers, host, vfile, lint) =>
        (maybeUser, maybeRepo) match {
          case (Some(user), Some(repo)) =>
            if(lint) {
              out.log.info("lsyncing project %s/%s@%s..." format(user, repo, vers))
              try {
                // todo: should this by an async server request?
                http(Client(host).lsync(user, repo, branch.getOrElse("master"), vers) as_str)
                out.log.info("Project was synchronized")
              } catch {
                case e =>
                  out.log.warn("Error synchronizing project libraries %s" format e.getMessage)
              }
            } else sys.error("Your version descriptor was invalid. %s" format(
              IO.read(vfile)
            ))
          case _ => sys.error("Could not resolve a Github git remote")
        }
    }

  private def writeVersionTask: Initialize[Task[Unit]] = 
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
          def sbtDefaultResolver(s: String) =
            s.contains("http://repo1.maven.org/maven2/") || s.contains("http://scala-tools.org/repo-releases")
          val resp = http(Client(DefaultLsHost).lib(lib, version)(user)(repo) as_str)
          val ls = inClassLoader(classOf[LibraryVersions]) { 
            parse[Seq[LibraryVersions]](resp)
          }

          // todo: refactorize
          // what's going on below is simply building up a pipeline of of settings
          // to ship off to sbt's to eval facitlity, then reload and optional save
          // the settings in the project's .sbt build definition
          val extracted = Project extract state
		      import extracted._
          import BuiltinCommands.{imports, reapply, DefaultBootCommands}
		      import CommandSupport.{DefaultsCommand, InitCommand}

          // one or more lines consisting of libraryDependency and resolvers
          // lhs is (plugin config, plugin instruct) | rhs is library config
          val lines: Either[(Seq[String], String), Seq[String]] = if(ls.size > 1) {
            sys.error("""More than one libraries were resolved.
                      | Try prefixing the library name with :user/:repo/:library-name to disambiguate.""".stripMargin)
          } else {
            val l = ls(0)
            (version match {
              case Some(v) => l.versions.find(_.version.equalsIgnoreCase(v))
              case _ => Some(l.versions(0))
            }) match {
              case Some(v) =>                
                if(l.sbt) {
                  println("Discovered sbt plugin %s@%s" format(l.name, v.version))
                  val depLine = ("""addSbtPlugin("%s" %% "%s" %% "%s")""" format(
                    l.organization, l.name, v.version)
                  ).trim

                  val rsvrLines = (v.resolvers.filterNot(sbtDefaultResolver).zipWithIndex.map {
                    case (url, i) =>
                      "resolvers += \"%s\" at \"%s\"".format(
                        "%s-resolver-%s".format(l.name, i),
                        url
                      )
                  })
                  val allLines = depLine +: rsvrLines
                  Left((allLines, PluginSupport.help(l, v, allLines)))                  
                } else {
                  println("Discovered library %s@%s" format(l.name, v.version))

                  Conflicts.detect(extracted.get(sbt.Keys.libraryDependencies), l, v)

                  val depLine = ("""libraryDependencies += "%s" %%%% "%s" %% "%s"%s""" format(
                    l.organization, l.name, v.version, config match {
                      case Some(c) => """ %% "%s" """.format(c)
                      case _ => ""
                    }
                  )).trim

                  val rsvrLines = (v.resolvers.filterNot(sbtDefaultResolver).zipWithIndex.map {
                    case (url, i) =>
                      "resolvers += \"%s\" at \"%s\"".format(
                        "%s-resolver-%s".format(l.name, i),
                        url
                      )
                  })
                  if(persistently) println("Evaluating and installling %s@%s"format(l.name, v.version))
                  else println("Evalutating %s@%s. Enter `session clear` to revert".format(
                    l.name, v.version
                  ))
                  Right(depLine +: rsvrLines)
                }

              case _ => sys.error("Could not find %s version of this library. possible versions (%s)" format(
                version.getOrElse("latest"), l.versions.mkString(", "))
              )
            }
          }

          lines.fold({
            // currently, we do not support plugin installation.
            // when we do, that magic should happen here
            case (pinLines, help) =>
              println(help)
              state
          }, { libLines =>
            // eval the first line to obtain
            // a starting point for a fold
            val (first, rest) = (libLines.head, libLines.tail)        
		        val settings = EvaluateConfigurations.evaluateSetting(
              session.currentEval(), "<set>", imports(extracted), first, 0
            )(currentLoader)
		        val append = Load.transformSettings(
              Load.projectScope(currentRef), currentRef.build, rootProject, settings
            )
		        val newSession = session.appendSettings( append map (a => (a, first)))
            // fold over lines to build up a new setting including all
            // setting appendings
            val (_, _, newestSession) = ((settings, append, newSession) /: rest)((a, line) => a match {
              case (set, app, ses) =>
                val settings = EvaluateConfigurations.evaluateSetting(
                  ses.currentEval(), "<set>", imports(extracted), line, 0
                )(currentLoader)
		          val append = Load.transformSettings(
                Load.projectScope(currentRef), currentRef.build, rootProject, set
              )
		          val newSession = ses.appendSettings( append map (a => (a, line)))
              (settings, append, newSession)
            })

		        val commands = DefaultsCommand +: InitCommand +: DefaultBootCommands
		        reapply(newestSession, structure,
              if(persistently) state.copy(remainingCommands = "session save" +: commands)
              else state)
          })
        } catch {
          case e: ClassNotFoundException  => sys.error(
            "class not found %s. This is likely to be a conflict betweek Jerkson and Sbt" format e.getMessage)
          case dispatch.StatusCode(404, msg) => sys.error(
            "Library not found %s" format msg
          )
          case p: ParsingException => sys.error(
            "received unexpected response from `ls`"
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
                  inClassLoader(classOf[LibraryVersions]) {
                    parse[Seq[LibraryVersions]](resp)
                  }, version(name), log)
              } catch {
                case e: ClassNotFoundException  =>
                  log.error("class not found %s. This is likely to be a conflict betweek Jerkson and Sbt" format e.getMessage)
                case StatusCode(404, msg) =>
                  log.info("`%s` library not found" format name)
                case p: ParsingException =>
                  log.info("received an unexpected response from `ls`")
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
                  inClassLoader(classOf[LibraryVersions]) {
                    parse[Seq[LibraryVersions]](resp)
                  }, log, color, name.split("@"):_*)
              } catch {
                case e: ClassNotFoundException  =>
                  log.error("class not found %s. This is likely to be a conflict betweek Jerkson and Sbt" format e.getMessage)
                case StatusCode(404, msg) =>
                  out.log.info("`%s` library not found" format name)
                case p: ParsingException =>
                  log.info("received unexpected response from `ls`")
              }
            case kwords =>
              val cli = Client(host)
              try {
                log.info("Fetching library info matching %s" format kwords.mkString(", "))
                val resp = http(cli.search(kwords.toSeq) as_str)
                libraries(
                  inClassLoader(classOf[LibraryVersions]) {
                    parse[Seq[LibraryVersions]](resp)
                  },
                  log, color,
                  args:_*
                )
              } catch {
                case e: ClassNotFoundException  =>
                  log.error("class not found %s. This is likely to be a conflict betweek Jerkson and Sbt" format e.getMessage)
                case StatusCode(404, msg) =>
                  log.info("Library not found for keywords %s" format kwords.mkString(", "))
                case p: ParsingException =>
                  log.info("received unexpected response from `ls`")
              }
          }
      }
    },
    (aggregate in lskey) := false,
    (aggregate in lsDocs) := false,
    commands ++= Seq(lsTry, lsInstall)
  )

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
    externalResolvers in lsync := Seq(ScalaToolsReleases),
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
        } catch { case e => Some(e) }
      case empty => None
    }


  private def libraries(libs: Seq[LibraryVersions], log: sbt.Logger, colors: Boolean, terms: String*) = {
    val tups = libs.map(l => (l.name, l.versions.map(_.version).mkString(", "), l.description))
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
      log.info(
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
