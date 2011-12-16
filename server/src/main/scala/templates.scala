package ls

object Templates {
  import unfiltered.response.Html
  import xml.NodeSeq

  val homeLink =
    <span class="home-link">
      So, what is everyone else <a href="/">building</a>?
    </span>

  def userLink(u: User) =
    <span class="user"><span class="at">@</span><a href={"/%s" format u.login }>{ u.login }</a></span>

  def docs(url: String) =
    url match {
      case "" => ""
      case ok => <a href={ok} target="_blank">Docs</a>
    }

  def site(url: String) =
    url match {
      case "" => ""
      case ok => <a href={ ok } target="_blank">Project Home</a>
    }

  def fullLibraryVersions = libraryVersions(true)_

  def shortLibraryVersions = libraryVersions(false)_

  def libraryVersions(full: Boolean)(l: LibraryVersions) =
    <div class="lib" id={ l.name }>
      <h2>
        <a href={ "/%s/%s/#%s" format(l.ghuser.get, l.ghrepo.get, l.name) }>
          { l.name }
        </a>
      </h2>
      <p class="description">{ l.description }</p>
      { if(full) libraryVersionsDetails(l) }
    </div>

  def libraryVersionsDetails(l: LibraryVersions) =
    <div>
      {
        if(!l.tags.isEmpty) <p class="tags">filed under { l.tags.map(tag) }</p>
      }
      <p class="external"><span>{ site(l.site) }</span></p>
      <div class="versions">
        <ul class="version-nums">{
          (l.versions.take(1), l.versions.drop(1)) match {
            case (first,  Nil) =>
             <li class="sel" id={ "%s-%s" format(
                 l.name, first(0).version.replaceAll("[.]", "-")
               )}>
               <a href="javascript:void(0)">{ first(0).version }</a>
             </li>
            case (first, rest) =>
              <li class="sel" id={ "%s-%s" format(
                  l.name, first(0).version.replaceAll("[.]", "-")
                )}>
                <a href="javascript:void(0)">{ first(0).version }</a>
              </li> ++ rest.map(v =>
              <li id={ "%s-%s" format(
                l.name, v.version.replaceAll("[.]", "-")
                ) }>
                <a href="javascript:void(0)">{ v.version }</a>
              </li>)
          }
        }</ul>
        <div class="version-details">{ 
          (l.versions.take(1), l.versions.drop(1)) match {
            case (first,  Nil) =>
             version(l, first(0), true)
            case (first, rest) =>
              version(l, first(0), true) ++ rest.map(v =>
                version(l, v, false)
              )
          }
        }
        </div>
      </div>
  </div>

  def dep(m: ModuleID) =
    <li>
      <span>{ m.name }</span>
      <span class="at">@</span>
      <span>{ m.version }</span>
    </li>

  def version(l: LibraryVersions, v: Version, sel: Boolean) =
    <div class={ "version %s" format(if(sel) "sel" else "") }
         id={ "v-%s-%s" format(l.name, v.version.replaceAll("[.]", "-")) }>
      <div class="section version-name">
        <h2>{ l.name } <span class="at">@</span> { v.version }</h2>
        <span>{ docs(v.docs) }</span>
      </div>
      <div class="section install">
        <h3>Install</h3>
        <div>
         {
           installInfo(
             ModuleID(l.organization, l.name, v.version),
             v.resolvers, v.scalas, l.sbt
           )
          }
        </div>
      </div>
      <div class="section scala-versions">
        <h3>Published for</h3><p>scala <span>{
          <ul>{ v.scalas.map(sv => <li>{ sv }</li>) }</ul>
        }</span></p>
      </div>
      <div class="section library-dependencies">
        <h3>Depends on</h3><span>{
          if(v.dependencies.isEmpty) "nothing"
          else <ul>{ v.dependencies.map(dep) }</ul>
        }</span>
      </div>
    </div>

  def tag(t: String) =
    <a href={ "/#%s" format t } class="tag"><span>{ t }</span></a>

 // fixme: not very robust all all
 def resolver(name: String, uri: String) = 
    "\nresolvers += \"%s\" at \"%s\"".format(name, uri)

 def sbtDefaultResolver(s: String) =
   s.contains("http://repo1.maven.org/maven2/") ||
   s.contains("http://scala-tools.org/repo-releases")

  def installInfo(mid: ModuleID, resolvers: Seq[String],
                  scalaVersions: Seq[String], sbtPlugin: Boolean) =
    if(sbtPlugin) installSbtPluginInfo(
      mid, resolvers: Seq[String], scalaVersions: Seq[String]
    ) else installLibraryInfo(
      mid, resolvers: Seq[String], scalaVersions: Seq[String]
    )

  def installSbtPluginInfo(mid: ModuleID, resolvers: Seq[String],
                           scalaVersions: Seq[String]) =
    <p>Add the following to your sbt plugins definition. <span class="clippy">{
     installSbtPluginText(mid, resolvers, scalaVersions)
    }</span></p> ++ { installSbtPlugin(mid, resolvers, scalaVersions) }

  def installSbtPluginText(mid: ModuleID, resolvers: Seq[String],
                           scalaVersions: Seq[String]) =
    "addSbtPlugin(\"%s\" %% \"%s\" %% \"%s\")\n%s".format(
      mid.organization, mid.name, mid.version, 
      (resolvers.filterNot(sbtDefaultResolver).zipWithIndex.map {
        case (r, i) => resolver("%s-resolver-%s".format(mid.name, i), r) }
      ).mkString("\n")
   )

  def installSbtPlugin(mid: ModuleID, resolvers: Seq[String],
                       scalaVersions: Seq[String]) =
    <pre><code>{ installSbtPluginText(mid, resolvers, scalaVersions) }</code></pre>

  def installLibraryInfo(mid: ModuleID, resolvers: Seq[String],
                         scalaVersions: Seq[String]) =
   <p>Add the following to your sbt build definition. <span class="clippy">{
     installLibraryText(mid, resolvers, scalaVersions)
   }</span></p> ++ { installLibrary(mid, resolvers, scalaVersions) }

 def installLibraryText(mid: ModuleID, resolvers: Seq[String],
                        scalaVersions: Seq[String]) =
   "libraryDependencies += \"%s\" %%%% \"%s\" %% \"%s\"\n%s".format(
      mid.organization, mid.name, mid.version, 
      (resolvers.filterNot(sbtDefaultResolver).zipWithIndex.map {
        case (r, i) => resolver("%s-resolver-%s".format(mid.name, i), r) }
      ).mkString("\n")
   )

  def installLibrary(mid: ModuleID, resolvers: Seq[String], scalaVersions: Seq[String]) =
    <pre><code>{ installLibraryText(mid, resolvers, scalaVersions) }</code></pre>

  def main(header: NodeSeq, content: NodeSeq, title: String = "ls")(scripts: String*)(sheets: String*) =
    layout(
      <div> { header } </div>
      <div id="content"> { content } </div>,
      title = title
    )(scripts:_*)(sheets:_*)

  val index = layout(
    <div id="index" class="block">
      <div id="search-content" class="vcentered">
        <h1><a href="/"><span class="ls">ls</span><span class="dot">.</span>implicit.ly</a></h1>
        <h2>
          a card catalog for
            <a href="https://scala-lang.org" target="_blank">scala</a> libraries
        </h2>
      <form id="q-form"><input type="search" autocomplete="off" id="q" name="q" /></form>
      <div id="libraries">
        <div class="content"></div>
        <div class="control"></div>
      </div>
      </div>
    </div>
  )("index")()

  def layout(body: xml.NodeSeq, title: String = "ls. a scala card catalog")(scripts: String*)(sheets: String*) = Html(
   <html>
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
      <title>{ title }</title>
      <link href="http://fonts.googleapis.com/css?family=Ubuntu+Condensed" rel="stylesheet" type="text/css"/>
      <link href="http://fonts.googleapis.com/css?family=Sorts+Mill+Goudy" rel="stylesheet" type="text/css"/>
      <link rel="stylesheet" type="text/css" href="/css/ls.css" />
      { sheets.map { s => <link rel="stylesheet" type="text/css" href="/css/%s.css" /> } }
      <script type="text/javascript" src="/js/jquery.min.js"></script>
      <script type="text/javascript"><![CDATA[
       var _gaq = _gaq || [];
        _gaq.push(['_setAccount', 'UA-26788051-1']);
        _gaq.push(['_trackPageview']);
       (function() {
        var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
        ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
        var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
       })();]]>
    </script>
    </head>
     <body>
       <div id="container">
         { body }
       </div>
        <div id="foot">
          Published under Scala. For the community.
          <div>
            <a href="#publishing">Publish your library</a> &bull; <a target="_blank" href="https://github.com/inbox/new?to=softprops">Contact your librarian</a>
          </div>
        </div> { Readme.body }
       <script type="text/javascript" src="/js/ls.js"></script>
       <script type="text/javascript" src="/js/time.js"></script>
       { scripts.map { s => <script type="text/javascript" src={"/js/%s.js" format s } /> } }
     </body>
   </html>
  )
}
