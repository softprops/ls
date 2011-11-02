package ls

object Browser extends Logged {
  import Templates._
  import ls.Libraries._
  import Conversions._
  import unfiltered._
  import request._
  import response._

  /** Paths for which we care not */
  def trapdoor: Cycle.Intent[Any, Any] = {
    case GET(Path("/favicon.ico")) => NotFound
  }

  def home: Cycle.Intent[Any, Any] = {
     case GET(Path("/")) => index
  }

  // Author
  def show: Cycle.Intent[Any, Any] = {
    case GET(Path(Seg(user :: Nil))) => Clock("show author %s" format user, log) {
      val (count, libsMarkup) =
        Libraries.author(user) { (libs: Iterable[LibraryVersions]) =>
          (libs.size, libs.map(libraryVersions))
        }
      divided(
        right = <div>
          <ul class="libraries">{ libsMarkup }</ul>
        </div>,
        left =
          <div class="author">
            <h1 class="author-name">
              <a href={ "/%s" format user }>{ user }</a>
            </h1>
            <h2 class="contributes">contributes to</h2>
            <p><span>{ if(count==0) "no" else <span class="num">{ count }</span> }</span> libraries</p>
            <p class="gh">
              on <a target="_blank" href={ "https://github.com/%s/" format user }>github</a>
            </p>
            <p>{ homeLink }</p>
          </div>,
          title = "ls /%s" format user
      )("jquery.clippy.min", "versions")()
    }

    // Projects
    case GET(Path(Seg(user :: repo :: Nil))) => Clock("show project %s/%s" format(user, repo), log) {
      val (libs, libMarkup) =
        Libraries.projects(user, Some(repo))({ (libs: Iterable[LibraryVersions]) =>
          (libs.toList, libs.map(libraryVersions))
        })
      divided(
        right = <ul>{ libMarkup }</ul>,
        left  = <div class="lib">
          <div class="head">
            <h1 class="project-name">
              <a href={"/%s/%s/" format(user, repo)}>{ repo }</a>
            </h1>
            <p class="gh">
              on <a target="_blank" href={"https://github.com/%s/%s/" format(user, repo)}>github</a>
            </p>
            <p class="by-line">
              <p>Contributors</p>
              <ul>
              { libs(0).contributors match {
                case Some(cs) => cs.map(c => <li>{ userLink(c) }</li>)
                case _ => <li>No contributors. Was this project authored by a ghost?</li>
              } }
              </ul>
            </p>
            <p>{ homeLink }</p>
          </div>
        </div>,
        title = "ls %s/%s" format(user, repo)
      )("jquery.clippy.min", "versions")()
    }
  }
}
