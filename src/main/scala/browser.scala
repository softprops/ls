package ls

object Browser {
  import unfiltered.request._
  import unfiltered.response._

  def home: unfiltered.Cycle.Intent[Any, Any] = {
     case GET(Path("/")) => view(
        <form action="/api/any" method="GET"><input type="text" id="q" name="q" /></form>
        <div><ul id="libraries"/></div>
     )("index")
  }

  def view(body: xml.NodeSeq)(scripts: String*) = Html(
   <html>
    <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
      <title>ls</title>
      <link rel="stylesheet" type="text/css" href="/css/ls.css" />
      <script type="text/javascript" src="/js/jquery.min.js"></script>
    </head>
     <body>
       <div id="container">
         <h1><a href="/">scala <span>ls</span></a></h1>
         { body }
       </div>
       <script type="text/javascript" src="/js/ls.js"></script>
       { scripts.map { s => <script type="text/javascript" src={"/js/%s.js" format s } /> } }
     </body>
   </html>
  )
}
