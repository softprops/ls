package ls

import sbt._
import Keys._
import Project.Initialize

object Plugin extends sbt.Plugin {
  import scala.collection.JavaConversions._
  import com.codahale.jerkson.Json._
  import com.codahale.jerkson.AST._
  import LsKeys._
  import java.io.File
  import java.net.URL
  import dispatch._
  import sbt.{ModuleID => SbtModuleID}

  // fixme. this only serves to make the repl usage more user-friendly
  val Ls = config("ls")

  object LsKeys {
    val colors = SettingKey[Boolean]("colors", "Colorize logging")
    val versionInfo = TaskKey[VersionInfo]("version-info", "Information about a version of a project")
    val versionFile = SettingKey[File]("File storing version descriptor file")
    val writeVersion = TaskKey[Unit]("write-version", "Writes version data to descriptor file")
    val lsync = TaskKey[Unit]("lsync", "Synchronizes github project info with ls server")
    val dependencyFilter = SettingKey[SbtModuleID => Boolean]("dependency-filter",
                                                              "Filters library dependencies included in version-info")
    // optional attributes
    val tags = SettingKey[Seq[String]]("tags", "List of taxonomy tags for the library")
    val docsUrl = SettingKey[Option[URL]]("doc-url", "Url for library documentation")
    val optionals = SettingKey[Optionals]("optionals", "Optional project info")

    val ghUser = SettingKey[Option[String]]("gh-user", "Github user name")
    val ghRepo = SettingKey[Option[String]]("gh-repo", "Github repository name")

    // api
    val lsHost = SettingKey[String]("ls-host", "Host ls server")
    val find = InputKey[Unit]("find",
                              "Search for libraries based on <user>, <user> <repo>, or <user> <repo> <version>")
    val search = InputKey[Unit]("search",
                             "Search for libraries based on arbitrary keywords")
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
     * version is optional and may be "latest" or a target version
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

  case class Lib(organization: String, name: String, versions: Seq[String], description: String)

  // use ast parsing b/c of clf issue w/ jerkson case classes + sbt
  def parseLibs(s: String): Seq[Lib] = (parse[JValue](s) match {
    case JArray(libs) => libs map {
      l => Seq(l \ "organization", l \ "name", l \ "versions" , l \ "description")
    }
    case _ => Seq.empty[JValue]
  }).map( _ match {
    case Seq(JString(o), JString(n), JArray(vs), JString(d)) =>
      Lib(o, n, vs.map(_ \ "version").map {
        case JString(s) => s
        case _ => sys.error(
          "Unexpected Version format"
        )
      }, d)
    case _ => sys.error("Unexpected Library format")
  })

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
          val ls = parseLibs(
            http(Client("http://localhost:5000").lib(lib, version, user, repo) as_str)
          )
          val line = if(ls.size > 1) {
            sys.error("More than one libraries resolved")
          } else {
            val l = ls(0)
            (version match {
              case Some(v) => l.versions.find(_.equalsIgnoreCase(v))
              case _ => Some(l.versions(0))
            }) match {
              case Some(v) =>
                ("""libraryDependencies += "%s" %%%% "%s" %% "%s"%s""" format(
                  l.organization, l.name, l.versions(0), config match {
                    case Some(c) => """ %% "%s" """.format(c)
                    case _ => ""
                  }
                )).trim
              case _ => sys.error("Could not find %s version of this library. possible versions (%s)" format(
                version.getOrElse("latest"), l.versions.mkString(", "))
              )
            }
          }
          val extracted = Project extract state
		      import extracted._
          import BuiltinCommands.{imports, reapply, DefaultBootCommands}
		      import CommandSupport.{DefaultsCommand, InitCommand}

		      val settings = EvaluateConfigurations.evaluateSetting(
            session.currentEval(), "<set>", imports(extracted), line, 0
          )(currentLoader)
		      val append = Load.transformSettings(
            Load.projectScope(currentRef), currentRef.build, rootProject, settings
          )
		      val newSession = session.appendSettings( append map (a => (a, line)))
		      val commands = DefaultsCommand +: InitCommand +: DefaultBootCommands
		      reapply(newSession, structure,
            if(persistently) state.copy(remainingCommands = "session save" +: commands)
            else state)
        } catch {
          case dispatch.StatusCode(404, msg) => sys.error(
            "Library not found %s" format msg
          )
        }
      case LibraryParser.Fail => sys.error(
        "Unexpected library format %s" format dep
      )
    }

  def lsTry = Command.single("ls-try")(depend(false))

  def lsInstall = Command.single("ls-install")(depend(true))

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
    optionals <<= (description in Ls, homepage in Ls, tags, docsUrl, licenses in Ls)((desc, homepage, tags, docs, lics) =>
       Optionals(desc, homepage, tags, docs, lics.map { case (name, url) => License(name, url.toString) })
    ),
    externalResolvers in Ls := Seq(ScalaToolsReleases),
    licenses in Ls <<= licenses in GlobalScope,
    versionInfo <<=
      (organization,
       name,
       version,
       optionals,
       externalResolvers in Ls,
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
    lsHost := "http://ls.implicit.ly",
    lsync <<= lsyncTask,
    (aggregate in lsync) := false,
    find <<= inputTask { (argsTask: TaskKey[Seq[String]]) =>
      (argsTask, streams, lsHost) map {
        case (args, out, host) =>
          val cli = Client(host)
          // todo: define query dsl
          args match {
            case Seq() => sys.error(
              "Please provide at least one search term"
            )
            case Seq(user) =>
              try {
                libraries(parseLibs(http(cli.author(user) as_str)),
                          out.log, user)
              } catch {
                case StatusCode(404, msg) =>
                  out.log.info("No libraries for user %s" format user)
              }
            case Seq(user, repo) =>
              try {
                libraries(parseLibs(http(cli.project(user, repo) as_str)),
                          out.log, user, repo)
              } catch {
                case StatusCode(404, msg) =>
                  out.log.info("No libraries for user %s and repo %s" format(user, repo))
              }
          }
      }
    },
    search <<= inputTask { (argsTask: TaskKey[Seq[String]]) =>
      (argsTask, streams, lsHost, state) map {
        case (args, out, host, state) =>
          args match {
            case Seq() => sys.error(
              "Please provide at lease one search keyword"
            )
            case kwords =>
              val cli = Client(host)
              libraries(
                parseLibs(http(cli.search(kwords.toSeq) as_str)),
                out.log,
                args:_*
              )
          }
      }
    },
    (aggregate in search) := false,
    (aggregate in find) := false
  )) ++ Seq(
    commands ++= Seq(lsTry, lsInstall)
  )

  trait Wheel[T] {
    def turn: Wheel[T]
    def get: T
  }

  def libraries(libs: Seq[Lib], log: sbt.Logger, terms: String*) = {
    val tups = libs.map(l => (l.name, l.versions.mkString(", "), l.description))
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
       case a => println("ls requires a git repository: %s" format a.toList);None
     } 
   }

  def maybeRepo: Option[(String, String)] = {
    (Process("git remote -v") !!).split("""\n""").collectFirst {
      case GhRepo(user, repo) => (user, repo)
    }
  }
}
