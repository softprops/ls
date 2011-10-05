package ls

import unfiltered._

object Server {
  import unfiltered.response._
  def main(a: Array[String]):Unit = {
    netty.Http(5000)
      .resources(getClass().getResource("/www/"))
      .handler(netty.cycle.Planify {
        Intentions.list orElse Intentions.find orElse Intentions.any orElse(
          Intentions.create orElse Browser.home
        )
     })
    .run(s => a match {
      case a@Array(_*) if(a contains "-b") => util.Browser.open(s.url)
      case _ => ()
     })
  }
}
