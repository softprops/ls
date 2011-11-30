package ls

import unfiltered._
import request._
import response._

object RequestLog extends Logged {
  import java.text.SimpleDateFormat
  import java.util.{Date => JDate, Locale}
  val DateFmt = "EEE, d MMM yyyy HH:mm:ss Z"
  
  def logRequest: Cycle.Intent[Any, Any] = {
    case r @ Path(path) =>
      log.info("%s %s %s" format(
        new SimpleDateFormat(DateFmt, Locale.US).format(
          new JDate()), r.method, path)
      )
      Pass
  }
}

object Api extends Logged {
  import Conversions._
  import Libraries._
  import QParams._
  
  def asJson(libs: Iterable[LibraryVersions]) =
    if(libs.isEmpty) NotFound
    else JsonContent ~> {
      log.info("returning %d libs" format libs.size)
      ResponseString(com.codahale.jerkson.Json.generate(libs))
    }

  def asJsonVersion(v: Iterable[String]) =
    v.iterator match {
      case it if(it.hasNext) =>
         JsonContent ~>
          ResponseString(
            """{"version":"%s"}""" format it.next
          )
      case _ => NotFound
    }

  def unparsable: PartialFunction[Github.Error, Boolean] = {
    case Github.Unparsable => true
    case _ => false
  }

  /** Synchronizes ls libraries with libraries on github.
   *  If projects are resolved but malformed, no persistence is made.
   *  Otherwise all project info is stored for later retrieval */
  def sync: Cycle.Intent[Any, Any] = {
    case POST(Path(Seg("api" :: "1" :: "libraries" :: Nil)) & Params(p)) =>
      val expect = for {
         user <- lookup("user") is required("missing")
         repo <- lookup("repo") is required("missing")
         branch <- lookup("branch") is optional[String, String]
         version <- lookup("version") is required("missing")
      } yield {
        log.info("synchronizing %s/%s/%s/%s" format(user.get, repo.get, branch.get.getOrElse("master"), version.get))
        Github.extract(user get, repo get, (branch get).getOrElse("master"), version get) match {
          case (e@Seq(_), Seq(libraries)) =>
            log.info("Some errors (%s), but some success(es)" format e)
            BadRequest ~> ResponseString(e map(_.msg) mkString(", "))
          case (e@Seq(_), _) =>
            log.info("All errs %s" format e)
            if(e.exists(unparsable))
              BadRequest ~> ResponseString(e map(_.msg) mkString(", "))
            else NotFound
          case (_, libraries) =>
            log.info("Resolved %d projects" format libraries.size)
            Libraries.save(libraries map(_.copy(ghuser = user, ghrepo = repo)))
            Created
          case _ => BadRequest
        }
      }
      Clock("lsync %s" format(p), log) {
        expect(p) orFail { errors =>
          BadRequest ~> ResponseString(
            errors map { fail => fail.name + ":" + fail.error } mkString ","
          )
        }
      }
  }

  /** Find libraries by ghuser (contributors included) */
  def authors: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "1" :: "authors" :: user :: Nil))) =>
      Clock("authors %s" format user, log) {
        Libraries.author(user)(asJson)
      }
  }

  /** Find libraries by projects */
  def projects: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "1" :: "projects" :: rest))) => Clock("projects %s" format rest, log) {
      rest match {
        case user :: Nil =>
          Libraries.projects(user)(asJson)
        case user :: repo :: Nil =>
          Libraries.projects(user, Some(repo))(asJson)
        case _ => NotFound
      }
    }
  }

  /** Find libraries by name and version */
  def libraries: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "1" :: "libraries" :: rest))) => Clock("libraries %s" format rest, log) {
      rest match {
        case name :: Nil =>
          Libraries(name)(asJson)
        case name :: version :: Nil =>
          Libraries(name, version = Some(version))(asJson)
        case name :: version :: user :: Nil =>
          Libraries(name, version = Some(version),
                  user = Some(user))(asJson)
        case name :: version :: user :: repo :: Nil =>
          Libraries(name, version = Some(version),
                  user = Some(user), repo = Some(repo))(asJson)
        case _ => NotFound
      }
    }
  }

  /** retrieves the latest version of a given library */
  def latest: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "1" :: "latest" :: lib :: Nil)) & Params(p)) =>
      val expect = for {
        user <- lookup("user") is optional[String, String]
        repo <- lookup("repo") is optional[String, String]
      } yield {
        import com.mongodb.DBObject
        import com.mongodb.casbah.Implicits._
        Libraries.latest(lib, user.get, repo.get)(asJsonVersion)(
          _.flatMap(Conversions.first(_)("versions", "version")).toList.headOption
        )
      }
      Clock("latest %s %s" format(lib, p), log) {
        expect(p) orFail { errors =>
           BadRequest ~> ResponseString(
             errors map { fail => fail.name + ":" + fail.error } mkString ","
           )
         }
      }
  }

  /** Find libraries by keywords */
  def search: Cycle.Intent[Any, Any] = {
     case GET(Path(Seg("api" :: "1" :: "search" :: Nil)) & Params(p)) =>
       val expect = for {
         q <- lookup("q") is required("missing")
         pg <- lookup("page") is optional[String, String]
         lim <- lookup("limit") is optional[String, String]
       } yield {
           Libraries.any(q.get.split("""\s+"""))(
             pg.get.getOrElse("1").toInt,
             lim.get.getOrElse("20").toInt
           )(asJson)
       }
       Clock("search %s" format p, log) {
         expect(p) orFail { errors =>
           BadRequest ~> ResponseString(
             errors map { fail => fail.name + ":" + fail.error } mkString ","
           )
         }
       }
  }

  /** Paginated sets of all libraries */
  def all: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "1" :: "libraries" :: Nil)) & Params(p)) =>
      val expect = for {
         pg <- lookup("page") is optional[String, String]
         lim <- lookup("limit") is optional[String, String]
      } yield {
        Libraries.all(
          pg.get.getOrElse("1").toInt,
          lim.get.getOrElse("20").toInt)(asJson)
      }
      Clock("all %s" format p, log) {
        expect(p) orFail { f =>
          NotFound
        }
      }
  }
}
