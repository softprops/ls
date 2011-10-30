package ls

object Readme {
  val body = 
    <div id="read">
      <div id="publishing">
      <h1><a href="/#publishing">publishing</a></h1>
      <h2>ls makes two assumptions about your Scala libraries</h2>
      <p>
        <ul>
          <li>1) Your library source is hosted on <a href="https://github.com">Github</a></li>
          <li>2) You build your projects with <a href="https://github.com/harrah/xsbt/wiki" target="_blank">sbt</a>*</li>
        </ul>
        * 2 is not required, but highly recommended.
      </p>
      <p>
        ls synchronizes your library's build information with a library version description file hosted on github, through a process called <code>lsyncing</code>. Lsyncing this information from sources publicly hosted on github means there is no need for authentication with ls's service. The author's commit access to the library implies their afflication with a given library.
      </p>
      <h2 id="lsync-spec">Lsync specification</h2>
      <p>
        ls stores semi-structured information about your library's versions by capturing a the contents of a json encoded file for each version of your library, tucking it away for later retrieval by others. The following is a description of the format that ls uses to capture this information.
      <pre><code>{{
 <span class="key">"organization"</span>:"org.yourdomain",             <span class="comment"># your mvn/ivy organization identifier</span>
 <span class="key">"name"</span>:"my-awesome-library",                 <span class="comment"># the name of your awesome library</span>
 <span class="key">"version"</span>:"1.0",                             <span class="comment"># the version of your awesome library</span>
 <span class="key">"description"</span>:"Hot stuff comin' through",    <span class="comment"># a quaint description of what it provides</span>
 <span class="key">"site"</span>:"http://yourdomain.org/awesome-library-overview", <span class="comment"># a uri for more info &amp; resources</span>
 <span class="key">"tags"</span>: ["awesome"],                          <span class="comment"># keywords to help others to find your library</span>
 <span class="key">"docs"</span>:"https://yourdomain.org/awesome-library-docs", <span class="comment"># a uri for api docs</span>
 <span class="key">"licenses"</span>: [{{                               <span class="comment"># how can others use your library</span>
   "name": "MIT", 
   "url":"https://yourdomain.org/awesome-library/LICENSE"
  }}],
 <span class="key">"resolvers"</span>: ["http://repo.yourdomain.org/"], <span class="comment"># when can others find your library</span>
 <span class="key">"library_dependencies"</span>: [{{                    <span class="comment"># what does your library depend on</span>
   "organization":"org.otherdomain",
   "name": "my-awesome-dependency",
   "version": "0.1.0"
  }}],
 <span class="key">"scala_versions"</span>: [                           <span class="comment"># what versions of scala is your library compiled for</span>
   "2.8.0","2.8.1","2.8.2",
   "2.9.0","2.9.0-1","2.9.1"
  ],                                 
 <span class="key">"sbt"</span>: false                                  <span class="comment"># is your library and sbt plugin?</span>
}}</code></pre>
      <p>To perform an lsync for a given project, simply perform the following http <code>POST</code></p>
      <pre><code>curl -X POST http://ls.implicit.ly/api/libraries 
  -F="user=your-gh-user"
  -F="repo=your-gh-repo"
  -F="version=vesrion-to-sync"</code></pre>
     ls will then recursively extract any files in the <code>github.com/your-gh-user/your-gh-repo</code> repository for files matching <code>src/main/ls/version-to-sync.json</code> and capture the above information.
      </p>
      <h2 id="plugin">Sbt plugin</h2>
      <p>The spec outlined above is a lot of information to author by hand. To make things simpler, just use the sbt plugin, <a href="/softprops/ls/#ls-sbt" target="_blank">ls-sbt</a> which will generate this for you. The purpose of the plugin is three-fold</p>
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
        From any project, just type <code>ls:write-version</code> and the plugin will generate a new version.json file for your library's current version.
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
        Sbt provides a means of declarativley defining <code>library dependencies</code> setting but requires you know a lot of information up front. ls can make that workflow even simplier.
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
  </div>
}
