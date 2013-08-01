package ls

import com.ning.http.client.{ Request, AsyncHandler }
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import java.net.UnknownHostException
import java.net.URLEncoder.encode
import java.nio.charset.Charset
import dispatch._
import org.json4s._

case class Client(host: String) {
  val utf8 = Charset.forName("utf-8")
  def api = url(host) / "api" / "1"

  implicit def pathMappableRequest(r: Req) = new {
    def /?(p: Option[String]) = p.map(r / _).getOrElse(r)
  }

  /** Find all projects by gh user */
  def author(name: String) =
    api / "authors" / name

  /** Find all projects by gh user and repo */
  def project(name: String, repo: String) =
    api / "projects" / name / repo

  /** Find all projects by gh user and repo */
  def version(name: String, repo: String, vers: String) =
    api / "projects" / name / repo / vers

  /** Find a target library by name and version, may return more than one if a published fork exists */
  def lib(
    name: String, version: Option[String] = None)
    (user: Option[String])
    (repo: Option[String]) =
      api / "libraries" / name /? version /? user /? repo

  /** Syncronize ls server with gihub library version info */
  def lsync(user: String, repo: String, branch: String, vers: String) =
    (api.POST / "libraries") << Map(
      "user" -> user,
      "repo" -> repo,
      "branch" -> branch,
      "version" -> vers
    )

  def search(kwords: Seq[String]) = 
    api / "search" <<? Map("q" -> kwords.mkString(" ").trim)

  /** Get the latest version number of a library */
  def latest(library: String,
             user: Option[String] = None,
             repo: Option[String] = None) =
    api / "latest" / library <<? (
      user.map { "user" -> _ } ::
      repo.map { "repo" -> _ } :: Nil
    ).flatten

  object Handler {
    def latest(
      library: String,
      user: Option[String] = None,
      repo: Option[String] = None
    ): (Request, AsyncHandler[Either[String, String]]) = {
      Client.this.latest(library, user, repo) OK (
        as.json4s.Json andThen { js =>
          (for {
            JObject(fs) <- js
            ("version", JString(version)) <- fs
          } yield version)
          .headOption
          .toRight("ls document is missing version property")
        }
      )
    }
  }
}

object DefaultClient {
  import dispatch.Defaults._

  // one shared instance
  val http = new Http

  // default host
  val client = Client("http://ls.implicit.ly/")

  /** Apply the request-response handler and return as Either */
  def apply[T](f: Client => (Request, AsyncHandler[T])): Either[String, Future[T]] = {
    import scala.util.control.Exception._    
    allCatch
      .either { http(f(client)) }
      .left.map {
        case uh: UnknownHostException =>
          "You may not know your host as well as you think. Your http client doesn't know %s" format uh.getMessage
        case StatusCode(404) =>
          "Can not find the resource requested (404)"
        case e =>
          "Unexpected http error %s" format e.getMessage
      }
  }
}
