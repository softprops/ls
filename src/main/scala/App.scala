package implicitly

import unfiltered.request.{GET, Path, Seg}
import unfiltered.response.ResponseString

object Intentions {
  def create[A, B]: unfiltered.Cycle.Intent[A, B] = {
    case GET(Path(Seg(user :: repo :: version :: Nil))) =>
      val resolved = ((Nil: Seq[Library]) /: Bench("extracting libs for %s/%s v%s" format(user, repo, version))(Github.extract(user, repo, version))) ((a, e) => e.fold(
        { err => a }, { lib => a:+ lib }
      ))
      resolved match {
        case Nil => ResponseString("Libraries Not Found")
        case xs => ResponseString(xs.toString)
      }
  }
}

object App extends unfiltered.filter.Planify(
  Intentions.create
)
