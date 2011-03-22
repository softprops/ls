package implicitly.models

object Projects {
  def mkKey(user: String, repo: String, org: String, module: String, version: String) =
    user :: repo :: org :: module :: version :: Nil mkString("/")

  def apply(id: (String, String, String), desc: String,
            resolvers: List[String],
            scalaVersions: List[String],
            parent: Option[(String, String,String)])(user: String, repo: String): Project = {
    val p = new Project()
    p.url = id match { case (org, module, version) =>
       mkKey(user, repo, org, module, version)
    }
    p.resolvers = resolvers.mkString(" ")
    p.scalaVersions = scalaVersions.mkString(" ")
    p.parentProject = parent match {
      case Some((org, module, version)) => mkKey(user, repo, org, module, version)
      case _ => "self"
    }
    p
  }
}
