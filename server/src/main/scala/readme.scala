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
          ls captures your library's build information and exposes it for others to easily find, through a process called <code>lsyncing</code>. Lsyncing synchonizes build information publicly hosted on Github. Because only priviledged parties have commit access to these repositories, there is no need to authenticate with ls.
      </p>
      <h2 id="lsync-spec">Lsync specification</h2>
      <p>
        ls stores semi-structured information about your library's in a json encoded file for each version of your library, tucking it safely away for later perusal by others. The following is a description of the format that ls uses to capture this information.
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
 <span class="key">"dependencies"</span>: [{{                           <span class="comment"># what does your library depend on</span>
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
     This will tell ls recursively extract any files in the <code>github.com/your-gh-user/your-gh-repo</code> repository for files matching <code>src/main/ls/version-to-sync.json</code> and capture the above information.
      </p>
      <p>No wants you to hand copy that for every library you write. That's what plugins are for.</p>
      <h3>ls-sbt plugin</h3>
      <p>To install a convenient ls client add the following to your projects, plugin definition</p>
      <pre><code>addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.0")</code></pre>
      <p>Then mix in the provided settings into your build definition</p>
      <pre><code>seq(lessSettings: _*)</code></pre>
      <p>When you are ready to sync your libraries build info with ls, do the following</p>
      <pre><code>sbt> ls:write-version</code></pre>
      <p>Then add or edit the generated file as you wish before commiting to git and pushing to your Github remote</p>
      <p>That's it. Go ahead and start working on the next version now.</p>
    </div>

    <div id="finding">
      <h1><a href="#finding">Thumbing through</a></h1>
      <p>There are lot of libraries out there, but they are not that easy to find as they could be. First off, need to know the name of the library then you need to rummage though its documentation to find information on versions an installation notes</p>
      <p>ls aims to make this as simple as it possibly can be</p>
      <pre><code>sbt> ls:find unfiltered</code></pre>
      <p>This will find any published library named unfiltered, listing recent versions and a library description</p>
      <p>What if you are looking for a specific version?</p>
      <pre><code>sbt> ls:find unfiltered@0.5.1</code></pre>
      <p>What if you don't know what a libraries name is? Try searcing by tags</p>
      <pre><code>sbt> ls:search web netty</code></pre>
    </div>

    <div id="installing">
       <h1><a href="installing">Checking the fit</a></h1>
       <p>Once you find the library you are looking for you have a few options. You can hand edit a configuration file, paste in the info you had to previous search for or you could do</p>
       <pre><code>sbt> ls-try unfiltered</code></pre>
       <p>This will temporarily add the latest version of unfiltered to your library chain. Try `console-quick` to play with the library in the repl. If you don't like it you can always remove it with <code>search clear</code> or reloading your project. If you do find the library that fits you need its just as easy to install it
       </p>
       <pre><code>sbt> ls-install unfiltered</code></pre>
       <p>The same syntax for specifying a specific version applies</p>
       <pre><code>sbt> ls-install unfiltered@0.5.1</code></pre>
    </div>
    <div id="uris">
      <h1><a href="#uris">You or I</a></h1>
      <p>Libraries are just references to uris in ls. More specifically a Github repository, a library name, and version.</p>
      <pre><code>sbt> ls-find library@0.5.0 user/repo</code></pre>
      <p>
        By default <code>try</code> and <code>install</code> will always use the latest version published.
      </p>
      <p>
        It is not recommended to lsync version information across repos but in the case that library author has, ls will prompt you for the repo you wish to use.
      </p>
    </div>
  </div>
}
