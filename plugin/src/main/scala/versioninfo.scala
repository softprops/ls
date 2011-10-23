package ls

import sbt.{Resolver, ModuleID => SbtModuleID,
          MavenRepository, JavaNet1Repository, SshRepository, SftpRepository,
          FileRepository, URLRepository, ChainedResolver, RawRepository}
import java.net.URL

case class Optionals(description: String, homepage: Option[URL], tags: Seq[String],
                     docsUrl: Option[URL], licenses: Seq[License])

case class VersionInfo(
  org: String, name: String, version: String, opts: Optionals,
  resolvers: Seq[Resolver], libraryDeps: Seq[SbtModuleID],
  scalaVersions: Seq[String], sbt: Boolean) {

  private def mjson(m: SbtModuleID) =
    """{
    |   "organization":"%s",
    |   "name": "%s",
    |   "version": "%s"
    |  }""".stripMargin.format(
      m.organization, m.name, m.revision
    )

  private def licjson(l: License) =
    """{
    |   "name": "%s",
    |   "url": "%s"
    |  }""".stripMargin.format(
      l.name, l.url
    )

  def represent = (_: Resolver) match {
    // Maven repos are composed of a name and root url,
    // we only care about capturing the url
    case repo: MavenRepository => repo.root
    case notsupported => sys.error(
      "ls currently only supports sbt MavenRespositories which %s is not. This may change in the future".format(
        notsupported
      )
    )
    //case repo: JavaNet1Repository => repo
    //case repo: SshRepository   => repo
    //case repo: SftpRepository  => repo
    //case repo: FileRepository  => repo
    //case repo: URLRepository   => repo
    //case repo: ChainedResolver => repo
    //case repo: RawRepository   => repo
  }

  lazy val json =
    """
    |{
    | "organization":"%s",
    | "name":"%s",
    | "version":"%s",
    | "description":"%s",
    | "site":"%s",
    | "tags":%s,
    | "docs":"%s",
    | "licenses": %s,
    | "resolvers": %s,
    | "library_dependencies": %s,
    | "scala_versions": %s,
    | "sbt": %s
    |}""".stripMargin.format(
      org, name, version,
      opts.description,
      opts.homepage.getOrElse(""),
      opts.tags.map("\"%s\"" format _).mkString("[",",","]"),
      opts.docsUrl.getOrElse(""),
      opts.licenses.map(licjson).mkString("[",",","]"),
      resolvers.map(r => "\"%s\"" format(represent(r))).mkString("[",",","]"),
      libraryDeps.map(mjson).mkString("[",",","]"),
      scalaVersions.map("\"%s\"" format _).mkString("[",",","]"),
      sbt
    )
}
