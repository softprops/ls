package implicitly

import unfiltered.request._
import unfiltered.response._

class App extends unfiltered.filter.Plan {
  import QParams._

  import models.Project
  import stores.ProjectStore

  def intent = {
    case GET(Path(Seg(user :: repo :: rest))) =>
      ((Github / (user, repo, rest.headOption map { _ + "/" })).right.flatMap {
        case Nil => Left("no package info found for %s/%s" format(user, repo))
        case files =>
          ((Right(Nil): Either[String,List[Project]]) /: files) {
            case (either, (name, src)) =>
              either.right.flatMap { cur =>
                 val p = Ls.Scala(src) map { _(user, repo) }
                 Right(p)
              }
          }
      }) fold ({ errs =>
        BadRequest ~> ResponseString(errs)
      }, { projects =>
        ProjectStore.save(projects.head) { (_:Either[String, Project]) fold( { err =>
          ResponseString(err)
        }, { saved =>
            Ok ~> ResponseString("Saved " +  saved.url)
        }) }
      })
  }
}
