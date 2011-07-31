package implicitly

object Main {
  def main(a: Array[String]) {
    unfiltered.jetty.Http(8080).filter(App).run(s =>
      unfiltered.util.Browser.open(s.url + "softprops/lsproject/0.1.0"))
  }
}
