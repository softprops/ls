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
  import LsKeys.{ ls => lskey, _ }
  import java.io.File
  import java.net.URL
  import dispatch._
  import sbt.{ ModuleID => SbtModuleID }
  import ls.LibraryVersions

  object LsKeys {
    // general
    val colors = SettingKey[Boolean](key("colors"), "Colorize logging")
    val host = SettingKey[String](key("host"), "Host ls server")
    val usage = TaskKey[Unit]("usage", "Displays usage information for tasks")

    // github
    val ghUser = SettingKey[Option[String]](key("gh-user"), "Github user name")
    val ghRepo = SettingKey[Option[String]](key("gh-repo"), "Github repository name")

    // syncing
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

    private def key(name: String) = "ls-%s" format name
  }

  val DefaultLsHost = "http://ls.implicit.ly"

  private def lsyncTask: Initialize[Task[Unit]] =
    (streams, ghUser, ghRepo, version in lsync, host in lsync) map {
      (out, maybeUser, maybeRepo, vers, host) =>
        (maybeUser, maybeRepo) match {
          case (Some(user), Some(repo)) =>
            out.log.info("lsyncing project %s/%s@%s..." format(user, repo, vers))
            try {
              http(Client(host).lsync(user, repo, vers) as_str)
              out.log.info("project was synchronized")
            } catch {
              case e =>
                out.log.warn("Error synchronizing project libraries %s" format e.getMessage)
            }
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

          val ls = inClassLoader(classOf[LibraryVersions]) { 
            parse[Seq[LibraryVersions]](
              http(Client(DefaultLsHost).lib(lib, version)(user)(repo) as_str)
            )
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
        |Usage: ls [-l] [terms...]
        |  
        |Examples
        |  # find a library named unfiltered
        |  ls -l unfiltered 
        |  # find a library named unfiltered at version of 0.5.1
        |  ls -l unfiltered@0.5.1
        |  # find libraries taged with terms web or http
        |  ls web http
        """.stripMargin)
    },
    lskey <<= inputTask { (argsTask: TaskKey[Seq[String]]) =>
      (argsTask, streams, host in lskey, colors in lskey, state) map {
        case (args, out, host, color, state) =>
          val log = out.log
          args match {
            case Seq() => sys.error(
              "Please provide at lease one or more search keywords or -l <name of library>"
            )
            case Seq("-l", name) =>
              val cli = Client(host)
              def named(name: String) = name.split("@") match {
                case Array(name) => cli.lib(name)_
                case Array(name, version) => cli.lib(name, version = Some(version))_
              }
              try {
                log.info("Fetching library info for %s" format name)
                libraries(
                  inClassLoader(classOf[LibraryVersions]) {
                    parse[Seq[LibraryVersions]](http(named(name)(None)(None) as_str))
                  }, log, color, name.split("@"):_*)
              } catch {
                case StatusCode(404, msg) =>
                  out.log.info("`%s` library not found" format name)
              }
            case kwords =>
              val cli = Client(host)
              try {
                log.info("Fetching library info matching %s" format kwords.mkString(", "))
                libraries(
                  inClassLoader(classOf[LibraryVersions]) {
                    parse[Seq[LibraryVersions]](http(cli.search(kwords.toSeq) as_str))
                  },
                  log, color,
                  args:_*
                )
              } catch {
                case StatusCode(404, msg) =>
                  log.info("library not found for keywords %s" format kwords.mkString(", "))
              }
          }
      }
    },
    (aggregate in lskey) := false,
    commands ++= Seq(lsTry, lsInstall)
  )

  /* https://github.com/harrah/xsbt/wiki/Configurations */
  def testDependency(m: sbt.ModuleID) =
    m.configurations match {
      case Some(conf) => conf.trim.toLowerCase.startsWith("test")
      case _ => false
    }

  def scalaLib(m: sbt.ModuleID) =
    m.organization.trim.toLowerCase.equals(
      "org.scala-lang"
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
    docsUrl := None,
    tags := Nil,
    description in lsync <<= (description in Runtime),
    homepage in lsync <<= (homepage in Runtime),
    optionals <<= (description in lsync, homepage in lsync, tags,
                   docsUrl, licenses in lsync)(
      (desc, homepage, tags, docs, lics) =>
        Optionals(desc, homepage, tags, docs, lics.map {
          case (name, url) => License(name, url.toString)
        }
      )
    ),
    skipWrite := false,
    externalResolvers in lsync := Seq(ScalaToolsReleases),
    licenses in lsync <<= licenses in GlobalScope,
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
    writeVersion <<= writeVersionTask,
    ghUser := (maybeRepo match {
      case Some((user, _)) => Some(user)
      case _ => None
    }),
    ghRepo := (maybeRepo match {
      case Some((_, repo)) => Some(repo)
      case _ => None
    }),
    lsync <<= lsyncTask,
    (aggregate in lsync) := false
  )

  def lsSettings: Seq[Setting[_]] = lsSearchSettings ++ lsPublishSettings

  def libraries(libs: Seq[LibraryVersions], log: sbt.Logger, colors: Boolean, terms: String*) = {
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

  object GhRepo {
    val GHRemote = """^git@github.com[:](\S+)/(\S+)[.]git$""".r
    def unapply(line: String) = line.split("""\s+""") match {
      case Array(_, GHRemote(user, repo), _) => Some(user, repo)
      case _ => None
    } 
  }

  def maybeRepo: Option[(String, String)] =
    Process("git remote -v").lines_!(ProcessLogging.silent).collectFirst {
      case GhRepo(user, repo) => (user, repo)
    }
}
