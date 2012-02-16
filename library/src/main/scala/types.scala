package ls

case class ModuleID(organization: String, name: String, version: String)
case class Dependencies(libraries: Seq[ModuleID])

case class User(login: String, id: Int, avatar_url: String, url: String)
case class License(name: String, url: String)

// api request representation
case class Library(organization: String, name: String, version: String,
                   description: String, site: String, tags: Seq[String],
                   docs: String, resolvers: Seq[String],
                   dependencies: Seq[ModuleID],
                   scalas: Seq[String],
                   licenses: Seq[License],
                   sbt: Boolean = false,
                   ghuser: Option[String] = None, ghrepo: Option[String] = None,
                   contributors: Option[Seq[User]] = None)

// internal representation
case class Version(
  version: String, docs: String,
  resolvers: Seq[String],
  dependencies: Seq[ModuleID],
  scalas: Seq[String],
  licenses: Seq[License],
  organization: String)// new since 0.1.2 the new preferred way to persist origanization info

case class LibraryVersions(
  organization: String, // deprecated in favor of allowing versions to declare organizations
  name: String,
  description: String, site: String, tags: Seq[String],
  updated: Long,
  versions: Seq[Version],
  sbt: Boolean = true,
  ghuser: Option[String] = None, ghrepo: Option[String] = None,
  contributors: Option[Seq[User]] = None)


