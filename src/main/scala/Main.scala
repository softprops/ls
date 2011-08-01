package implicitly

object Main {
  def main(a: Array[String]) {
    unfiltered.netty.Http(8080).handler(unfiltered.netty.cycle.Planify(Intentions.list)).handler(unfiltered.netty.cycle.Planify(Intentions.create)).run(s =>
      unfiltered.util.Browser.open(s.url + "libraries"))
  }
}
