package ls

object Git {
  
  object GhRepo {
    val GHRemote = """^git@github.com[:](\S+)/(\S+)[.]git$""".r
    def unapply(line: String) = line.split("""\s+""") match {
      case Array(_, GHRemote(user, repo), _) => Some(user, repo)
      case _ => None
    } 
  }

  val CurrentBranch = """^([*]\S+)(.*)$""".r

  lazy val cli =
    sys.props.get("os.name")
      .filter(_.toLowerCase.contains("windows"))
      .map(_ => "git.exe")
      .getOrElse("git")

  def branch: Option[String] =
    try {
      sbt.Process("%s branch" format Git.cli)
        .lines_!(ProcessLogging.silent).collectFirst {
          case CurrentBranch(_, br) => br
        }
    } catch {
      case _ => None
    }

  def ghRepo: Option[(String, String)] =
    try {
      sbt.Process("%s remote -v" format Git.cli)
        .lines_!(ProcessLogging.silent).collectFirst {
          case GhRepo(user, repo) => (user, repo)
        }
    } catch {
      case _ => None
    }
}
