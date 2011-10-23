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
    else JsonContent ~>
      ResponseString(com.codahale.jerkson.Json.generate(libs))

  def unparsable: PartialFunction[Github.Error, Boolean] = {
    case Github.Unparsable => true
    case _ => false
  }

  /** Synchronizes ls libraries with libraries on github.
   *  If projects are resolved, but malformed not persistence is made.
   *  Otherwise all project info is stored for later retrival */
  def sync: Cycle.Intent[Any, Any] = {
    case POST(Path(Seg("api" :: "libraries" :: Nil)) & Params(p)) =>
      val expect = for {
         user <- lookup("user") is required("missing")
         repo <- lookup("repo") is required("missing")
         version <- lookup("version") is required("missing")
      } yield {
        log.info("synchronizing %s/%s/%s" format(user.get, repo.get, version.get))
        Github.extract(user get, repo get, version get) match {
          case (e@Seq(_), Seq(libraries)) =>
            log.info("some err somes success(es)")
            BadRequest ~> ResponseString(e map(_.msg) mkString(", "))
          case (e@Seq(_), _) =>
            log.info("all errs")
            if(e.exists(unparsable))
              BadRequest ~> ResponseString(e map(_.msg) mkString(", "))
            else NotFound
          case (_, libraries) =>
            log.info("Resolved %d projects" format libraries.size)
            Libraries.save(libraries map(_.copy(ghuser = user, ghrepo = repo)))
            Created ~> Redirect("/")
          case _ => BadRequest
        }
      }
      expect(p) orFail { errors =>
        BadRequest ~> ResponseString(
          errors map { fail => fail.name + ":" + fail.error } mkString ","
        )
      }
  }

  /** Find libraries by ghuser (contributors included) */
  def authors: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "authors" :: user :: Nil))) =>
      Libraries.author(user)(asJson)
  }

  /** Find libraries by projects */
  def projects: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "projects" :: rest))) => rest match {
      case user :: Nil =>
        Libraries.projects(user)(asJson)
      case user :: repo :: Nil =>
        Libraries.projects(user, Some(repo))(asJson)
      case _ => NotFound
    }
  }

  /** Find libraries by name and version */
  def libraries: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "libraries" :: rest))) => rest match {
      case name :: Nil => // just the name (latest implied)
        Libraries(name)(asJson)
      case name :: version :: Nil => // name with version 
        Libraries(name/*, Some(version)*/)(asJson)
      case _ => NotFound
    }
  }

  /** Find libraries by keywords */
  def search: Cycle.Intent[Any, Any] = {
     case GET(Path(Seg("api" :: "search" :: Nil)) & Params(p)) =>
       val expect = for {
         q <- lookup("q") is required("missing")
       } yield {
           Libraries.any(q.get.split("""\s+"""))()(asJson)
       }
       expect(p) orFail { errors =>
         BadRequest ~> ResponseString(
          errors map { fail => fail.name + ":" + fail.error } mkString ","
        )
      }
  }

  // Paginated sets of all libraries
  def all: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "libraries" :: Nil)) & Params(p)) =>
      log.info("list %s" format p)
      val expect = for {
         pg <- lookup("page") is optional[String, String]
      } yield {
        Libraries.all()(asJson)
      }
      expect(p) orFail { f =>
        NotFound
     }
  }
}
