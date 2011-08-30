package ls

object Server {
  import unfiltered.response._
  def main(a: Array[String]):Unit = {
    unfiltered.netty.Http(4567).
    //context("/assets"){ _.resources(getClass().getResource("/www/")) }
    resources(getClass().getResource("/www/"), cacheSeconds = 0, passOnFail = true)
    //.handler(unfiltered.netty.cycle.Planify {
    .handler(unfiltered.netty.cycle.Planify {
      Intentions.list orElse Intentions.find orElse Intentions.any orElse Intentions.create orElse Browser.home
     })
    .run(s => a match {
      case a@Array(_*) if(a contains "-b") => unfiltered.util.Browser.open(s.url)
      case _ => ()
     })
  }
}
