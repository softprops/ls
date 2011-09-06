package ls

import unfiltered.request._
import unfiltered.response._

object Intentions {
  import Libraries._
  import QParams._

  def asJson(libs: Iterable[Library]) =
    JsonContent ~>
      ResponseString(com.codahale.jerkson.Json.generate(libs))

  def asSbt(libs: Iterable[Library]) =
    CharContentType("application/x-sbt") ~> ResponseString(libs.map(l =>
      Sbt.configuration(l)
    ).mkString("\n!BUILD\n"))

  def unparsable: PartialFunction[Github.Error, Boolean] = {
    case Github.Unparsable => true
    case _ => false
  }

  def create: unfiltered.Cycle.Intent[Any, Any] = {
    case POST(Path("/api/libraries") & Params(p)) =>
      val expect = for {
         user <- lookup("user") is required("missing")
         repo <- lookup("repo") is required("missing")
         version <- lookup("version") is required("missing")
      } yield {
        Github.extract(user get, repo get, version get) match {
          case (e@Seq(_), Seq(libraries)) =>
            println("some err some success")
            BadRequest ~> ResponseString(e map(_.msg) mkString(", "))
          case (e@Seq(_), _) =>
            println("all errs")
            if(e.exists(unparsable))
              BadRequest ~> ResponseString(e map(_.msg) mkString(", "))
            else NotFound
          case (_, libraries) =>
            println("success")
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

  def find: unfiltered.Cycle.Intent[Any, Any] = {
    case GET(Path(Seg("api" :: "l" :: rest))) => rest match {
      case user :: Nil =>
        Libraries(user)(asJson)
      case user :: repo :: Nil =>
        Libraries(user, Some(repo))(asJson)
      case user :: repo :: "latest" :: Nil =>
        Libraries(user, Some(repo),  Some("latest"))(asJson)
      case user :: repo :: version :: Nil =>
        Libraries(user, Some(repo), Some(version))(asJson)
      case _ => NotFound
    }
  }

  def any: unfiltered.Cycle.Intent[Any, Any] = {
     case GET(Path(Seg("api" :: "any" :: Nil)) & Params(p)) =>
       val expect = for {
         q <- lookup("q") is required("missing")
       } yield {
           Libraries.any(q.get)()(asJson)
       }
       expect(p) orFail { errors =>
         BadRequest ~> ResponseString(
          errors map { fail => fail.name + ":" + fail.error } mkString ","
        )
      }
  }

  def list: unfiltered.Cycle.Intent[Any, Any] = {
    case GET(Path("/api/libraries") & Params(p)) =>
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
