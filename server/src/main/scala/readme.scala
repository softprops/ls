package ls

object Readme {
  val body = 
    <div id="read">
      <div id="publishing">
      <h1><a href="/"><span class="ls">ls</span><span class="dot">.</span>implicit.ly</a> : <a href="/readme#publishing">publishing</a></h1>
      <h2>ls makes two assumptions about your Scala libraries</h2>
      <p>
        <ul>
          <li>1) Your library is hosted on <a href="https://github.com">Github</a></li>
          <li>2) You build your projects with <a href="https://github.com/harrah/xsbt/wiki" target="_blank">sbt</a>*</li>
        </ul>
        * 2 is not required, but highly recommended.
      </p>
      <p>
        ls synchronizes your library's build information with a library version description file hosted on github, through a process called <code>lsyncing</code>. Lsyncing this information from sources publicly hosted on github means there is no need for authentication with ls's service.
      </p>
      <h2 id="lsync-spec">Lsync specification</h2>
      <p>
        ls stores semi structured information about your library versions by capturing a json encoded file for each version of your library and tucks it for later retrieval by others. The following is a description of the format that ls uses to capture this information.
      <pre><code>{{
 "organization":"org.yourdomain", <span class="comment">// your mvn/ivy organization identifier</span>
 "name":"my-awesome-library", <span class="comment">// the name of your awesome project</span>
 "version":"1.0", <span class="comment">// the version of you awesome project</span>
 "description":"Hot stuff comin' through", <span class="comment">// a short description that describes your project</span>
 "site":"http://yourdomain.org/awesome-library-overview", <span class="comment">// where we can find more info on your project</span>
 "tags": ["awesome"], <span class="comment">// tags help categorize your library for others to find</span>
 "docs":"https://yourdomain.org/awesome-library-docs", <span class="comment">// where we can find documentation on your projects api</span>
 "licenses": [{{ <span class="comment">// how do you want to distribute your library</span>
   "name": "MIT", 
   "url":"https://yourdomain.org/awesome-library/LICENSE"
  }}],
 "resolvers": ["http://repo.yourdomain.org/"], <span class="comment">// how can we resolve your library</span>
 "library_dependencies": [{{ <span class="comment">// what does your library depend on</span>
   "organization":"org.otherdomain",
   "name": "my-awesome-dependency",
   "version": "0.1.0"
  }}],
 "scala_versions": [
   "2.8.0","2.8.1","2.8.2",
   "2.9.0","2.9.0-1","2.9.1"
  ], <span class="comment">// what versions of scala is your library compiled for</span>
 "sbt": false <span class="comment">// is your library and sbt plugin?</span>
}}</code></pre>
      <p>To perform an lsync for a given repository simply perform an http <code>POST</code> to ls's <code>libraries</code> path</p>
      <pre><code>curl -X POST http://ls.implicit.ly/api/libraries 
  -F="user=your-gh-user"
  -F="repo=your-gh-repo"
  -F="version=vesrion-to-sync"</code></pre>
     ls will then recursively extract any files in the <code>github.com/your-gh-user/your-gh-repo</code> repository for files matching <code>src/main/ls/version-to-sync.json</code> and capture the above information.
      </p>
      <h2 id="plugin">Sbt plugin</h2>
      <p>This is a lot of information to author by hand. To make things simple, just use the sbt plugin, <a href="#" target="_blank">ls-sbt</a>. The purpose of the plugin is three-fold</p>
      <p>
        <ul>
          <li>1) To make it easy to lsync your scala library's version info</li>
          <li>2) To make it easy to find scala libraries</li>
          <li>3) To make it easy to install scala libraries</li>
        </ul>
      </p>
      <p>
       To install the plugin just add the following to your <code>plugins.sbt</code> file.
       <pre><code>addSbtPlugin("me.lessis" %% "ls-sbt" % "0.1.0")</code></pre>
      </p>
      <h2>Lsync'ing from sbt</h2>
      <p>
        From any project just type <code>ls:write-version</code> and the plugin will generate a new version.json file for your library's current version.
        When you are ready to publish your library's version information, commit and push your version info file to Github then, from sbt, type
        <pre><code>sbt> ls:lsync</code></pre>
       If all goes well, you will then be able to find your library on ls.implicit.ly.
      </p>
      <h2>Finding libraries from sbt</h2>
      <p>
        You can find libraries by project or by keywords from sbt....
      </p>
      <h2>Installing scala libraries from sbt</h2>
      <p>
        Sbt already provides a means installing libraries through its <code>libraryDependencies</code> setting but requires you know a lot of information up front. ls can make that workflow even simplier.
        ls provides functionality for both <code>trying</code> and <code>installing</code> scala libraries.
      </p>
      <p>
        Once you've found a library you want to install, simply type
        <pre><code>sbt> ls-try unfiltered-netty-server</code></pre>
        This will temporarily install a published library until you restart sbt. To persist this installation just type
        <pre><code>sbt> ls-install unfiltered-netty-server</code></pre>
      </p>
      <p>
        Libraries are just references to uris in ls. To install a specific version type
        <pre><code>sbt> ls-install unfiltered-netty-server@0.5.0</code></pre>
        By default <code>try</code> and <code>install</code> will always use the latest version published.
      </p>
      <p>
        It is not recommended to lsync version information across repos but in the case that library author has, ls will prompt you for the repo you wish to use.
      </p>
    </div>
    <div id="contacting">
     <h1><a href="/"><span class="ls">ls</span><span class="dot">.</span>implicit.ly</a> : <a href="/readme#contacting">contacting</a></h1>
     <h2>Questions</h2>
     <p>Have a question? <a href="https://github.com/inbox/new?to=softprops" target="_blank">Send me a message on github</a>. My name is <a href="https://github.com/softprops" target="_blank">softprops</a>. I'm friendly</p>
     <h2>Issues</h2>
     <p>Have an issue or suggestion? <a href="https://github.com/softprops/ls/issues/new" target="_blank">Post one on github</a></p>
    </div>
  </div>
}
