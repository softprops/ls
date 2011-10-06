package ls

case class Client(host: String) {
  import dispatch._
  lazy val api = url(host) / "api"
  /** Find all projects by gh user */
  def user(name) = api / "l" / user
  /** Find all projects by gh user and repo */
  def repository(name: String, repo: String) =
    api / "l" / name / repo
  /** Find all projects by gh user and repo */
  def version(name: String, repo: String, vers: String) =
    api / "l" / name / repo / vers
}
