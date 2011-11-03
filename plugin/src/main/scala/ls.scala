package ls

import sbt._
import Keys._
import Project.Initialize

object Plugin extends sbt.Plugin {
  import ls.ClassLoaders._
  import org.apache.http.conn.{HttpHostConnectException => ConnectionRefused}
  import scala.collection.JavaConversions._
  import com.codahale.jerkson.Json._
  import com.codahale.jerkson.AST._
  import LsKeys._
  import java.io.File
  import java.net.URL
  import dispatch._
  import sbt.{ModuleID => SbtModuleID}
  import ls.LibraryVersions

  object LsKeys {
    val colors = SettingKey[Boolean](key("colors"), "Colorize logging")
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

    val ghUser = SettingKey[Option[String]](key("gh-user"), "Github user name")
    val ghRepo = SettingKey[Option[String]](key("gh-repo"), "Github repository name")

    // api
    val host = SettingKey[String](key("host"), "Host ls server")
    val find = InputKey[Unit](key("find"),
                              "Search for libraries based on <user>, <user> <repo>, or <user> <repo> <version>")
    val search = InputKey[Unit](key("search"),
                             "Search for libraries based on arbitrary keywords")

    private def key(name: String) = "ls-%s" format name
  }

  private def http[T](hand: Handler[T]): T = {
    val h = new Http
    try { h(hand) }
    catch {
      case cf:ConnectionRefused => sys.error(
        "ls is currently not available to take your call"
      )
      case uh:java.net.UnknownHostException => sys.error(
        "You may not know your host as well as you think. Your http client doesnt know %s" format uh.getMessage
      )
    }
    finally { h.shutdown() }
  }

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
     (streams, versionFile, versionInfo) map {
      (out, f, info) =>
        if(!f.exists) {
          f.getParentFile().mkdirs()
          out.log.debug("Writing %s to %s" format(info.json, f))
          IO.write(f, info.json)
        } else Prompt.ask(
          "Version info for %s@%s already exists? To you wish to override it? [Y/N] " format(
            info.name, info.version
          )) { r =>
            val a = r.trim.toLowerCase
            if(Prompt.Yes contains a) {
              out.log.debug("writing %s to %s" format(info.json, f))
              IO.write(f, info.json)
            }
            else if(Prompt.No contains a) out.log.info("Canceling request")
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
   *   ls-try unfiltered-netty@latest
   *   ls-try unfiltered/unfiltered/unfiltered-netty@0.4.2
   *   ls-try
   */
  private def depend(persistently: Boolean)(state: State, dep: String) =
    LibraryParser(dep) match {
      case LibraryParser.Pass(user, repo, lib, version, config) =>
        try {
          def sbtDefaultResolver(s: String) =
            s.contains("http://repo1.maven.org/maven2/") || s.contains("http://scala-tools.org/repo-releases")

          val ls = inClassLoader(classOf[LibraryVersions]) { 
            parse[Seq[LibraryVersions]](
              http(Client("http://localhost:5000").lib(lib, version)(user)(repo) as_str)
            )
          }
          // one or more lines consisting of libraryDependency and resolvers
          val lines: Seq[String] = if(ls.size > 1) {
            sys.error("More than one libraries resolved")
          } else {
            val l = ls(0)
            (version match {
              case Some(v) => l.versions.find(_.version.equalsIgnoreCase(v))
              case _ => Some(l.versions(0))
            }) match {
              case Some(v) =>

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
                depLine +: rsvrLines

              case _ => sys.error("Could not find %s version of this library. possible versions (%s)" format(
                version.getOrElse("latest"), l.versions.mkString(", "))
              )
            }
          }
          val extracted = Project extract state
		      import extracted._
          import BuiltinCommands.{imports, reapply, DefaultBootCommands}
		      import CommandSupport.{DefaultsCommand, InitCommand}

          val (first, rest) = (lines.head, lines.tail)
          
		      val settings = EvaluateConfigurations.evaluateSetting(
            session.currentEval(), "<set>", imports(extracted), first, 0
          )(currentLoader)
		      val append = Load.transformSettings(
            Load.projectScope(currentRef), currentRef.build, rootProject, settings
          )
		      val newSession = session.appendSettings( append map (a => (a, first)))

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
            }
          )


		      val commands = DefaultsCommand +: InitCommand +: DefaultBootCommands
		      reapply(newestSession, structure,
            if(persistently) state.copy(remainingCommands = "session save" +: commands)
            else state)
        } catch {
          case dispatch.StatusCode(404, msg) => sys.error(
            "Library not found %s" format msg
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

  def lsSettings: Seq[Setting[_]] = Seq(
    colors := true,
    version in lsync <<= (version in Runtime)(_.replace("-SNAPSHOT","")),
    sourceDirectory in lsync <<= (sourceDirectory in Compile)( _ / "ls"),
    versionFile <<= (sourceDirectory in lsync, version in lsync)(_ / "%s.json".format(_)),
    dependencyFilter := { m => m.organization != "org.scala-lang" },
    docsUrl := None,
    tags := Nil,
    description in lsync <<= (description in Runtime),
    homepage in lsync <<= (homepage in Runtime),
    optionals <<= (description in lsync, homepage in lsync, tags, docsUrl, licenses in lsync)((desc, homepage, tags, docs, lics) =>
       Optionals(desc, homepage, tags, docs, lics.map { case (name, url) => License(name, url.toString) })
    ),
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
       crossScalaVersions) map { (o, n, v, opts, rsvrs, ldeps, dfilter, csv, pi) =>
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
    (host in lsync) := "http://ls.implicit.ly",
    lsync <<= lsyncTask,
    (aggregate in lsync) := false,
    find <<= inputTask { (argsTask: TaskKey[Seq[String]]) =>
      (argsTask, streams, host in lsync) map {
        case (args, out, host) =>
          val cli = Client(host)
          // todo: define query dsl
          def named(name: String) = name.split("@") match {
            case Array(name) => cli.lib(name)_
            case Array(name, version) => cli.lib(name, version = Some(version))_
          }
          args match {
            case Seq() => sys.error(
              """Please provide at least one argument.
                |usage: name@version user repo
                | @version, user and repo are all optional
                |""".stripMargin
            )
            case Seq(name) =>
              try {
                libraries(
                  inClassLoader(classOf[LibraryVersions]) {
                    parse[Seq[LibraryVersions]](http(named(name)(None)(None) as_str))
                  }, out.log, name.split("@"):_*)
              } catch {
                case StatusCode(404, msg) =>
                  out.log.info("%s library not found" format name)
              }
            case Seq(name, user) =>
              try {
                 libraries(
                   inClassLoader(classOf[LibraryVersions]) {
                     parse[Seq[LibraryVersions]](http(named(name)(Some(user))(None) as_str))
                   }, out.log, name.split("@") :+ user: _*)
              } catch {
                case StatusCode(404, msg) =>
                  out.log.info("%s library not found for %s" format(name, user))
              }
            case Seq(name, user, repo) =>
              try {
                libraries(
                  inClassLoader(classOf[LibraryVersions]) {
                    parse[Seq[LibraryVersions]](http(named(name)(Some(user))(Some(repo)) as_str))
                  }, out.log, name.split("@") :+ user :+ repo: _*)
              } catch {
                case StatusCode(404, msg) =>
                  out.log.info("%s library not found for %s/%s" format(name, user, repo))
              }
          }
      }
    },
    search <<= inputTask { (argsTask: TaskKey[Seq[String]]) =>
      (argsTask, streams, host in lsync, state) map {
        case (args, out, host, state) =>
          args match {
            case Seq() => sys.error(
              "Please provide at lease one search keyword"
            )
            case kwords =>
              val cli = Client(host)
              libraries(
                inClassLoader(classOf[LibraryVersions]) {
                  parse[Seq[LibraryVersions]](http(cli.search(kwords.toSeq) as_str))
                },
                out.log,
                args:_*
              )
          }
      }
    },
    (aggregate in search) := false,
    (aggregate in find) := false,
    commands ++= Seq(lsTry, lsInstall)
  )

  trait Wheel[T] {
    def turn: Wheel[T]
    def get: T
  }

  def libraries(libs: Seq[LibraryVersions], log: sbt.Logger, terms: String*) = {
    val tups = libs.map(l => (l.name, l.versions.map(_.version).mkString(", "), l.description))
    val len = math.max(tups.map{ case (n, vs, _ ) => "%s (%s)".format(n, vs).size }.sortWith(_>_).head, 10)
    val fmt = "%s %-" + len + "s # %s"

    val c = "\033[0;32m" :: "\033[0;33m" :: "\033[0;34m" :: "\033[0;35m" :: "\033[0;36m" ::  Nil
    val lterms: Seq[String] = terms.toList
    def colorWheel(opts: Seq[String]): Wheel[String] = new Wheel[String] {
      def turn = opts match {
        case head :: tail => colorWheel(tail ::: head :: Nil)
        case head => colorWheel(head)
      }
      def get = opts(0)
    }

    def hl(txt: String, terms: Seq[String],
           cw: Wheel[String] = colorWheel(c)): String =
      terms match {
        case head :: Nil =>
          txt.replaceAll("""(?i)(\?\S+)?(""" + head + """)(\?\S+)?""", cw.get + "$0\033[0m").trim
        case head :: tail =>
          hl(txt.replaceAll("""(?i)(\?\S+)?(""" + head + """)(\\S+)?""", cw.get + "$0\033[0m").trim,
             tail, cw.turn)
        case Nil => txt
      }

    val clrs = util.Random.shuffle(c)
    tups.zipWithIndex map { case ((n, v, d), i) =>
      log.info(
        hl(fmt format(
          "%s)" format i,
          "%s (%s)".format(n,v),
          d
        ), lterms, colorWheel(clrs))
      )
    }
    if(libs.isEmpty) log.info("(no projects matching the terms %s)" format terms.mkString(" "))
  }

  object GhRepo {
    val GHRemote = """^git@github.com[:](\S+)/(\S+)[.]git$""".r
     def unapply(line: String) = line.split("""\s+""") match {
       case Array(_, GHRemote(user, repo), _) => Some(user, repo)
       case a => /*println(
         "ls requires a github git remote: Your current remotes are: \n\t%s" format (
             a.toList.mkString("\n\t")
         )
       )*/
       None
     } 
   }

  def maybeRepo: Option[(String, String)] = {
    (Process("git remote -v") !!).split("""\n""").collectFirst {
      case GhRepo(user, repo) => (user, repo)
    }
  }
}
