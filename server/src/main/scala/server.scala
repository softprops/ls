package ls

import unfiltered._

object Server {
  import unfiltered.response._
  def main(a: Array[String]):Unit = {
    netty.Http(Props.int("PORT").getOrElse(5000))
      .resources(getClass().getResource("/www/"))
      .handler(netty.cycle.Planify {
        Browser.trapdoor
      })
      .handler(netty.cycle.Planify{
        RequestLog.logRequest
      })
      .handler(netty.cycle.Planify {
        Api.all orElse Api.latest orElse Api.projects orElse Api.search orElse(
          Api.sync orElse Api.authors orElse Api.libraries orElse Browser.home orElse Browser.show
        )
      })
      .run(s => a match {
       case a@Array(_*) if(a contains "-b") => util.Browser.open(s.url)
       case _ => ()
      })
  }
}
