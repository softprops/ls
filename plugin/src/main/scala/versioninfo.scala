package ls

import sbt.{ Resolver, ModuleID => SbtModuleID,
          MavenRepository, JavaNet1Repository, SshRepository, SftpRepository,
          FileRepository, URLRepository, ChainedResolver, RawRepository }
import java.net.URL

case class Optionals(
  description: String, homepage: Option[URL],
  tags: Seq[String], docsUrl: Option[URL],
  licenses: Seq[License])

case class VersionInfo(
  org: String, name: String, version: String, opts: Optionals,
  resolvers: Seq[Resolver], libraryDeps: Seq[SbtModuleID],
  scalaVersions: Seq[String], sbt: Boolean) {

  import com.codahale.jerkson.Json._
  import ls.{ Library, ModuleID => LsModuleID, License }
  import pj.Printer

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

  def sbtToLsModuleID(smid:SbtModuleID) = 
    LsModuleID(smid.organization, smid.name, smid.revision)

  lazy val json =
    Printer(generate(Library(org, name, version,
          opts.description, opts.homepage.map(_.toString).getOrElse(""),
          opts.tags, opts.docsUrl.map(_.toString).getOrElse(""),
          resolvers.map(represent), libraryDeps.map(sbtToLsModuleID),
          scalaVersions, opts.licenses, sbt)))
          .fold({ sys.error }, { pjs => pjs })
}
