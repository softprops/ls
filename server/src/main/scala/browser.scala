package ls

object Browser {
  import unfiltered.request._
  import unfiltered.response._

  def home: unfiltered.Cycle.Intent[Any, Any] = {
     case GET(Path("/")) => view(
        <form action="/api/any" method="GET">
         <input type="text" id="q" name="q" />
         <p class="help">n:name | v:version | u:github-user | r:github-repostory </p>
       </form>
        <div><ul id="libraries"/></div>
     )("index")
  }

  def view(body: xml.NodeSeq)(scripts: String*) = Html(
   <html>
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
      <title>ls</title>
      <link href="http://fonts.googleapis.com/css?family=Andada" rel="stylesheet" type="text/css"/>
      <link href="http://fonts.googleapis.com/css?family=Ubuntu+Mono" rel="stylesheet" type="text/css"/>
      <link href="http://fonts.googleapis.com/css?family=Ubuntu+Condensed" rel="stylesheet" type="text/css"/>
      <link rel="stylesheet" type="text/css" href="/css/ls.css" />
      <script type="text/javascript" src="/js/jquery.min.js"></script>
    </head>
     <body>
       <div id="container">
         <h1><a href="/"><span>ls</span><span>.</span>implicit.ly</a></h1>
         { body }
       </div>
       <script type="text/javascript" src="/js/ls.js"></script>
       { scripts.map { s => <script type="text/javascript" src={"/js/%s.js" format s } /> } }
     </body>
   </html>
  )
}
