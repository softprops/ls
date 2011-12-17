package ls

case class Client(host: String) {
  import dispatch._
  import java.net.URLEncoder.encode
  val utf8 = java.nio.charset.Charset.forName("utf-8")
  lazy val api = url(host) / "api" / "1"

  implicit def pathMappableRequest(r: dispatch.Request) = new {
    def /?(p: Option[String]) = p.map(r / _).getOrElse(r)
  }

  /** Find all projects by gh user */
  def author(name: String) = api / "authors" / name
  /** Find all projects by gh user and repo */
  def project(name: String, repo: String) =
    api / "projects" / name / repo
  /** Find all projects by gh user and repo */
  def version(name: String, repo: String, vers: String) =
    api / "projects" / name / repo / vers
  /** Find a target library by name and version, may return more than one if a published fork exists */
  def lib(name: String, version: Option[String] = None)(
    user: Option[String])(repo: Option[String]) =
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
    def latest(library: String,
               user: Option[String] = None,
               repo: Option[String] = None) =
      Client.this.latest(library, user, repo) >- { js =>
        scala.util.parsing.json.JSON.parseFull(js).map {
          _.asInstanceOf[Map[String,Any]]("version").toString
        }
      }
  }
}

object DefaultClient {
  import dispatch._
  val client = Client("http://ls.implicit.ly/")
  /** Apply the request-response handler and return as Either */
  def apply[T](f: Client => Handler[T]) = {
    val http = new Http with NoLogging
    try {
      Right(http(f(client)))
    } catch {
      case e => Left(e)
    } finally {
      http.shutdown()
    }
  }
}
