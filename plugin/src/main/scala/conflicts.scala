package ls

import sbt._
import sbt.Keys._

object Conflicts {

  case class Conflict(installed: sbt.ModuleID, targetLibrary: LibraryVersions,
                      targetVersion: Version, message: String) extends RuntimeException(message)

  // performs error side effect throwing a `Conflict` if collistion is detected
  def detect(current: Seq[sbt.ModuleID], lib: LibraryVersions, version: Version) =
    current.find( m =>
      m.name.equalsIgnoreCase(lib.name) &&
      m.organization.equalsIgnoreCase(lib.organization)
    ) match {
      case Some(installed) =>
        throw Conflict(installed, lib, version,
          """A potential conflict was detected between a currently installed library dependency (%s@%s)
          |and the library you are intending to install (%s@%s)
          |You may wish to uninstall version %s of this dependency first.""".stripMargin.format(
            installed.name, installed.revision,
            lib.name, version.version, installed.revision
          )
      )
      case _ => ()
    }
}
