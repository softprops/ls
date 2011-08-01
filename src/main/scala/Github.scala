package implicitly

case class ModuleID(organization: String, name: String, version: String)
case class Dependencies(projects: Seq[ModuleID], libraries: Seq[ModuleID])
case class Library(organization: String, name: String, version: String,
                   description: String, tags: Seq[String],
                   docs: String, resolvers: Seq[String],
                   dependencies: Dependencies,
                   ghuser: Option[String] = None, ghrepo: Option[String] = None)
case class Blob(content: String, encoding: String)
case class Entry(path: String, mode: String, `type`: String, size: Option[Int], sha: String, url: String)
case class Tree(sha: String, url: String, tree: Seq[Entry])

object Github {
  import dispatch._
  import com.codahale.jerkson.Json._
  import util.control.Exception.allCatch

  private def http[T](h: => Handler[T]): T = {
     val call = new Http with HttpsLeniency
     try { Bench("http request")(call(h)) }
     finally { call.shutdown() }
  }

  private def lib(user: String, repo: String, sha: String): Either[String, Library] = try {
    val s = System.currentTimeMillis
    http(repos.secure / user / repo / "git" / "blobs" / sha <:< Map("Accept" -> "application/vnd.github-blob.raw") >> { in =>
      println("gh responded blob res in  %sms" format(System.currentTimeMillis - s))
      Bench("parsing %s blob" format sha)(Right(parse[Library](in)))
    })
  } catch {
    case dispatch.StatusCode(code, _) => Left("Not Found")
  }

  def extract(user: String, repo: String, version: String): List[Either[String, Library]] = try {
    val s = System.currentTimeMillis
    http(repos.secure / user / repo / "git" / "trees" / "master" <<? Map("recursive" -> "1") >> { in =>
      println("gh responded to tree request in %sms" format(System.currentTimeMillis - s))
      Bench("parsing tree for %s/%s" format(user, repo))(parse[Tree](in)).tree
        .filter(_.path.matches("""(\S+)?src/main/ls/%s.json$""".format(version))) match {
          case Nil       => Left("Not Found") :: Nil
          case l :: Nil  => Bench("extracting one lib %s" format l.sha)(lib(user, repo, l.sha)) :: Nil
          case ls        => Bench("folding over %s libraries" format ls.size)(((Nil: List[Either[String, Library]]) /: ls)((a,l) =>
            Bench("extracting lib for %s" format l.sha)(lib(user, repo, l.sha)) ::  a
          ))
        }
    })
  } catch { case e =>
    Left("Not found %s" format e) :: Nil
  }

  private val repos = :/("api.github.com").secure / "repos"
}
