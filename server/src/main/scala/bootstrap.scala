package ls

object Bootstrap {
  val databinder= "net.databinder"
  def user(name: String) = User(
    name, 0, "", ""
  )

  def generate = {
    val libs = Seq(
      Library(databinder,"unfiltered-netty", "0.5.1",
              "Unfiltered http abstractions for Netty nio servers",
              "http://unfiltered.databinder.net/Unfiltered.html",
              Seq("web","nio", "http", "async","unfiltered"),
              "http://unfiltered.databinder.net/Unfiltered.html",
              Seq("http://databinder.net/repo"),
              Seq(ModuleID(databinder, "unfiltered", "0.5.1")),
              Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1"),
              Seq(License("MIT", "https://github.com/unfiltered/unfiltered/blob/master/LICENSE")),
              false,
              Some("unfiltered"), Some("unfiltered"),
              Some(Seq(user("n8han"), user("softprops")))
      ),
      Library(databinder,"unfiltered", "0.5.1",
              "Toolkit for servicing HTTP requests in scala",
              "http://unfiltered.databinder.net/Unfiltered.html",
              Seq("web","http","unfiltered"),
              "http://unfiltered.databinder.net/Unfiltered.html",
              Seq("http://databinder.net/repo"),
              Seq(ModuleID(databinder, "unfiltered", "0.5.1")),
              Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1"),
              Seq(License("MIT", "https://github.com/unfiltered/unfiltered/blob/master/LICENSE")),
              false,
              Some("unfiltered"), Some("unfiltered"),
              Some(Seq(user("n8han"), user("softprops")))
      ),
      Library(databinder,"unfiltered-filter", "0.5.1",
              "Unfiltered extractors abstraction for HttpServletFilters",
              "http://unfiltered.databinder.net/Unfiltered.html",
              Seq("web","http","unfiltered", "filter"),
              "http://unfiltered.databinder.net/Unfiltered.html",
              Seq("http://databinder.net/repo"),
              Seq(ModuleID(databinder, "unfiltered", "0.5.1")),
              Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1"),
              Seq(License("MIT", "https://github.com/unfiltered/unfiltered/blob/master/LICENSE")),
              false,
              Some("unfiltered"), Some("unfiltered"),
              Some(Seq(user("n8han"), user("softprops")))
      ),
      Library(databinder,"unfiltered-agents", "0.5.1",
              "Unfiltered request extractors for intelligently identifying user agents",
              "http://unfiltered.databinder.net/Unfiltered.html",
              Seq("web","http","unfiltered", "filter"),
              "http://unfiltered.databinder.net/Unfiltered.html",
              Seq("http://databinder.net/repo"),
              Seq(ModuleID(databinder, "unfiltered", "0.5.1")),
              Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1"),
              Seq(License("MIT", "https://github.com/unfiltered/unfiltered/blob/master/LICENSE")),
              false,
              Some("unfiltered"), Some("unfiltered"),
              Some(Seq(user("n8han"), user("softprops")))
      ),
       Library("me.lessis", "ls-sbt", "0.1.0",
              "an sbt interface for ls.implicit.ly",
              "",
              Seq("ls","plugin","sbt"),
              "",
              Seq("http://repo.lessis.me"),
              Seq(
                ModuleID("com.codahale", "jerkson", "0.5.0"),
                ModuleID("net.databinder", "dispatch-http", "0.8.5")
              ),
              Seq("2.9.1"),
              Seq(License("MIT", "https://github.com/unfiltered/unfiltered/blob/master/LICENSE")),
              true,
              Some("softprops"), Some("ls"),
              Some(Seq(user("n8han"), user("softprops")))
      )
    )
      
    Libraries.save(libs)
  }
}
