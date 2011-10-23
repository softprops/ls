package ls

case class ModuleID(organization: String, name: String, version: String)
case class Dependencies(libraries: Seq[ModuleID])

case class User(login: String, id: Int, avatar_url: String, url: String)
case class License(name: String, url: String)
// api request representation
case class Library(organization: String, name: String, version: String,
                   description: String, site: String, tags: Seq[String],
                   docs: String, resolvers: Seq[String],
                   library_dependencies: Seq[ModuleID],
                   scala_versions: Seq[String],
                   licenses: Seq[License],
                   sbt: Boolean = false,
                   ghuser: Option[String] = None, ghrepo: Option[String] = None,
                   contributors: Option[Seq[User]] = None)

// internal representation
case class Version(version: String, docs: String,
                   resolvers: Seq[String],
                   library_dependencies: Seq[ModuleID],
                   scala_versions: Seq[String],
                   licenses: Seq[License])

case class LibraryVersions(organization: String, name: String,
                           description: String, site: String, tags: Seq[String],
                           updated: Long,
                           versions: Seq[Version],
                           sbt: Boolean = true,
                           ghuser: Option[String] = None, ghrepo: Option[String] = None,
                           contributors: Option[Seq[User]] = None)


