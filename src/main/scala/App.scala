package implicitly

import unfiltered.request.{GET, Path, Seg}
import unfiltered.response.ResponseString

object Intentions {

  def create[A, B]: unfiltered.Cycle.Intent[A, B] = {
    case GET(Path(Seg(user :: repo :: version :: Nil))) =>
      (((Nil: Seq[Library]) /: Bench("extracting libs for %s/%s v%s" format(user, repo, version))(Github.extract(user, repo, version))) ((a, e) => e.fold(
        { err => a }, { lib => a:+ lib }
      ))) match {
        case Nil => ResponseString("Libraries Not Found")
        case xs =>
          Bench("Saving libs")(Libraries.save(xs map { _.copy(ghuser = Some(user)).copy(ghrepo = Some(repo)) }))
          Bench("Getting libs")(Libraries.all { col => ResponseString(col.toString) })
      }
  }

  def list[A, B]: unfiltered.Cycle.Intent[A, B] = {
     case GET(Path("/libraries")) => Bench("Getting libs")(Libraries.all { col => ResponseString(col.toString) })
  }
}
