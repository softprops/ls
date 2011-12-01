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
          (libs.size, libs.map(shortLibraryVersions))
        }
      main(
        header =
          <h1 class="author-name">
            <a href="/"><span class="ls">ls</span></a> <span class="sl">/</span> <a href={ "/%s" format user }>{ user }</a>
          </h1>
         <div class="head-extra author-extra">
            <h2 class="contributes">
              contributes to <span>{ if(count==0) "no" else <span class="num">{ count }</span> }</span> { if(count==1) "library" else "libraries" } on <a target="_blank" href={ "https://github.com/%s/" format user }>Github</a> including
            </h2>
          </div>,
        content = <div>
          <ul class="libraries">{ libsMarkup }</ul>
        </div>,
        title = "ls / %s" format user
      )("jquery.clippy.min", "versions")()
    }

    // Projects
    case GET(Path(Seg(user :: repo :: Nil))) => Clock("show project %s/%s" format(user, repo), log) {
      val (libs, libMarkup) =
        Libraries.projects(user, Some(repo))({ (libs: Iterable[LibraryVersions]) =>
          (libs.toList, libs.map(fullLibraryVersions))
        })
      main(
        header =
          <h1 class="project-name">
            <a href="/"><span class="ls">ls</span></a> <span class="sl">/</span> <a href={"/%s/%s/" format(user, repo)}>{ repo }</a>
          </h1>
          <div class="head-extra project-extra">
            on <a target="_blank" href={"https://github.com/%s/%s/" format(user, repo)}>Github</a> with contributions from
            <div id="contributors">
              <ul>
              { libs(0).contributors match {
                case Some(cs) =>
                  cs.map(c => <li>{ userLink(c) }</li>)
                case _ =>
                  <li>No contributors. Was this project authored by a ghost?</li>
              } }
              </ul>
            </div>
          </div>,
        content = <ul>{ libMarkup }</ul>,
        title = "ls / %s / %s" format(user, repo)
      )("jquery.clippy.min", "versions")()
    }
  }
}
