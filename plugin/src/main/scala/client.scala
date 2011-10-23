package ls

case class Client(host: String) {
  import dispatch._
  import java.net.URLEncoder.encode
  val utf8 = java.nio.charset.Charset.forName("utf-8")
  lazy val api = url(host) / "api"
  /** Find all projects by gh user */
  def author(name: String) = api / "authors" / name
  /** Find all projects by gh user and repo */
  def project(name: String, repo: String) =
    api / "projects" / name / repo
  /** Find all projects by gh user and repo */
  def version(name: String, repo: String, vers: String) =
    api / "projects" / name / repo / vers
  /** Find a target library by name and version, may return more than one if a published fork exists */
  def lib(name: String, version: Option[String] = Some("latest"), user: Option[String] = None,
          repo: Option[String] = None) = (user, repo) match {
    case (Some(u), Some(r)) => api / "libraries" / name / version.getOrElse("latest") / u / r
    case _ => api / "libraries" / name / version.getOrElse("latest")
  }
  def search(kwords: Seq[String]) = 
    api / "search" <<? Map("q" -> kwords.mkString(" ").trim)
}
