package ls

trait ManagedHttp {
  import dispatch._
  protected def http[T](handler: => Handler[T]): T = {
     val client = new Http with HttpsLeniency
     try { client(handler) }
     finally { client.shutdown() }
  }
}

// note classes w/i an object that mixes a trait breaks jerkson
case class Blob(content: String, encoding: String)
case class Entry(path: String, mode: String, `type`: String, size: Option[Int], sha: String, url: String)
case class Tree(sha: String, url: String, tree: Seq[Entry])

object Github extends ManagedHttp {
  import dispatch._
  import com.codahale.jerkson.Json._
  import util.control.Exception.allCatch

  case class Error(msg: String)
  case object NotFound extends Error("Not Found")
  case object Unparsable extends Error("Unparsable")

  private val Target = """(\S+)?src/main/ls/%s.json$"""
  private val BlogOpts = Map("Accept" -> "application/vnd.github-blob.raw")
  private val TreeOpts = Map("recursive" -> "1")

  /** Extract all libraries from a repo turning a seq of Either[Error, Lib] to (Seq[Error], Seq[Lib]) */
  def extract(user: String, repo: String, version: String): (Seq[Error], Seq[Library]) =
    ((Seq.empty[Error], Seq.empty[Library]) /: any(user, repo, version)) ((a, e) => a match {
        case (l, r) => (e.left.toSeq ++ l, e.right.toSeq ++ r)
     })

  /** Extract a library from a gh blob, result may be unparsable or not found */
  private def lib(user: String, repo: String, sha: String): Either[Error, Library] =
    try {
      http(repos.secure / user / repo / "git" / "blobs" / sha <:< BlogOpts >> { in =>
        try { Right(parse[Library](in)) }
        catch { case _ => println("%s was unparsable" format sha);Left(Unparsable) }
      })
    } catch {
      case _ => println("%s not found" format sha);Left(NotFound)
    }

  /** Extract all libraries from a repo */
  private def any(user: String, repo: String, version: String): Seq[Either[Error, Library]] =
    allCatch.opt {
      http(repos.secure / user / repo / "git" / "trees" / "master" <<? TreeOpts >> { in =>
        parse[Tree](in).tree.filter(_.path.matches(Target.format(version))) match {
          case Nil       => Left(NotFound) :: Nil
          case l :: Nil  => lib(user, repo, l.sha) :: Nil
          case ls        => ls.map(l => lib(user, repo, l.sha))
        }
      })
    }.getOrElse(Seq(Left(NotFound)))

  private val repos = :/("api.github.com").secure / "repos"
}
