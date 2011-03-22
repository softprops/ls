package implicitly

object Github {
  import dispatch._
  import dispatch.liftjson.Js._
  import net.liftweb.json.JsonAST._
  import scala.util.control.Exception.allCatch

  trait SilentLogging {
    def httpLogger = new dispatch.Logger { def info(msg: String, args: Any*) {} }
  }

  object GaeHttp extends SilentLogging {
    implicit def http = new dispatch.gae.Http {
      override def make_logger = httpLogger
    }
  }
  import GaeHttp._

  object Name {
    def apply(name: String, module: Option[String]) =
      ("^%sls.json$".format(module.getOrElse("")).r).findFirstMatchIn(name) match {
        case Some(n) => Some(n)
        case _ => None
      }
  }

  /** @returns either[error, List[(name, src)]] */
  def /(user: String, repo: String, module: Option[String]) =
    allCatch.opt { http(api / "blob" / "all" / user / repo / "master" ># { js =>
      for {
        blobs <- ('blobs ? obj)(js)
        JField(name, JString(sha)) <- blobs
        name <- Name(name, module)
      } yield {
        (name, http(api / "blob" / "show" / user / repo / sha as_str))
      }
    }) }.toRight {
      "Could not resolve data for %s/%s %s" format(user, repo, module)
    }

  private val api = :/("github.com").secure / "api" / "v2" / "json"

}
