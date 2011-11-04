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
          ls learns about your library through a process called <code>lsyncing</code>, which synchronizes with build information publicly hosted on Github. Because only privileged parties may commit to these repositories, ls is not concerned with who is requesting an lsync—that is, authentication—but with the controlled project metadata on Github.
      </p>
      <h2 id="lsync-spec">Lsync spec</h2>
      <p>
        ls stores semi-structured information about your library's build in a json encoded file for each version of your library. The following is a description of the format that ls uses to capture this information.
      <pre><code>{{
 <span class="comment"># a maven/ivy organization identifier</span>
 <span class="key">"organization"</span>:"org.yourdomain",
 <span class="comment"># name of your awesome library</span>
 <span class="key">"name"</span>:"my-awesome-library",
 <span class="comment"># version of your awesome library</span>
 <span class="key">"version"</span>:"1.0",
 <span class="comment"># quaint description of what it provides</span>
 <span class="key">"description"</span>:"Hot stuff comin' through",
 <span class="comment"># a uri for more info &amp; resources</span>
 <span class="key">"site"</span>:"http://yourdomain.org/awesome-library-overview",
 <span class="comment"># keywords to help others to find your library</span>
 <span class="key">"tags"</span>: ["awesome", "opossum"],
 <span class="comment"># a uri for api docs</span>
 <span class="key">"docs"</span>:"https://yourdomain.org/awesome-library-docs",
 <span class="comment"># liceneses to ill</span>
 <span class="key">"licenses"</span>: [{{
   "name": "MIT",
   "url":"https://yourdomain.org/awesome-library/LICENSE"
  }}],
 <span class="comment"># when can others find your library</span>
 <span class="key">"resolvers"</span>: ["http://repo.yourdomain.org/"],
 <span class="comment"># what does your library depend on</span>
 <span class="key">"dependencies"</span>: [{{
   "organization":"org.otherdomain",
   "name": "my-awesome-dependency",
   "version": "0.1.0"
  }}],
 <span class="comment"># cross-binary dialects</span>
 <span class="key">"scalas"</span>: [
   "2.8.0","2.8.1","2.8.2",
   "2.9.0","2.9.0-1","2.9.1"
  ],
 <span class="comment"># is this an sbt plugin?</span>
 <span class="key">"sbt"</span>: false
}}</code></pre>
      <p>To perform an <code>lsync</code> for a given project, simply perform the following HTTP <code>POST</code></p>
      <pre><code>curl -X POST http://ls.implicit.ly/api/libraries 
  -F="<span class="key">user</span>=your-gh-user"
  -F="<span class="key">repo</span>=your-gh-repo"
  -F="<span class="key">version</span>=version-to-sync"</code></pre>
     This will tell ls to recursively extract any files in the <code>github.com/your-gh-user/your-gh-repo</code> repository for files matching <code>src/main/ls/version-to-sync.json</code> and capture the above information.
      </p>
      <p>No wants you to hand copy that for every library you write. That's what plugins are for.</p>
      <h3>ls-sbt plugin</h3>
      <p>To install a convenient ls client, add the following to your projects plugin definition</p>
      <pre><code>addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.0")
resolvers += "coda" at "http://repo.codahale.com"
</code></pre>
      <p>Then mix in the provided settings into your build definition</p>
      <pre><code>seq(lsSettings: _*)</code></pre>
      <p>Add or edit the generated file under <code>src/main/ls/:version.json</code> to fit your liking before commiting to git and pushing to your Github remote</p>
      <p>When you are ready to sync your libraries build info with ls, do the following</p>
      <pre><code>sbt> ls-write-version</code></pre>
      <p>That's it! Free free to go ahead and start banging out the next awesome version of your library now.</p>
    </div>

    <div id="finding">
      <h1><a href="#finding">Thumbing through</a></h1>
      <p>There are lot of libraries out there, but they are not as easy to find as they could be. For starters, you need to know the name of the library, then you need to rummage though its (hopefully well-documented) documentation to find version information and installation notes.</p>
      <p>ls aims to make finding Scala libraries as simple as it possibly can be by adding some vocabulary to your sbt shell</p>
      <pre><code>sbt> ls-find unfiltered</code></pre>
      <p>This command will find any ls-published library named <code>unfiltered</code>, listing recent versions with descriptions</p>
      <p>What if you are looking for a specific version? Just add <code>@:version</code> to your query</p>
      <pre><code>sbt> ls-find unfiltered@0.5.1</code></pre>
      <p>What if you don't know what a libraries name is at all? Try searcing by tags and see what pops up.</p>
      <pre><code>sbt> ls-search web netty</code></pre>
    </div>

    <div id="installing">
       <h1><a href="installing">Checking the fit</a></h1>
       <p>Once you find the library you are looking for, you have a few options. You can hand edit a configuration file, paste in the info you had to previous search for or you could do</p>
       <pre><code>sbt> ls-try unfiltered</code></pre>
       <p>This will temporarily add the latest version of unfiltered to your library chain. Try <code>console-quick</code> to play with the library in the repl. If you don't like it you can always remove it with the <code>session clear</code> command or by reloading your project. If you do find the library that fits you need, it's just as easy to install it.
       </p>
       <pre><code>sbt> ls-install unfiltered</code></pre>
       <p>The same syntax for specifying a specific version applies</p>
       <pre><code>sbt> ls-install unfiltered@0.5.1</code></pre>
    </div>
    <div id="uris">
      <h1><a href="#uris">You or I</a></h1>
      <p>Because of the open nature of Github, a forked repository can coexist with another of the same name under a different user's account. Libraries is ls are just references to <code>uris</code>. More specifically a uri for Github repository, a library name, and version.</p>
      <pre><code>sbt> ls-find library@0.5.0 user/repo</code></pre>
      <p>
        By default <code>try</code> and <code>install</code> will always use the latest version of a library by name using an inferred uri.
      </p>
      <p>
        It is not recommended to lsync version information across repos. Doing so may decrease the likelihood of your users finding the right library. In the case a library author has lsync'd across repositories, ls will prompt you for the repo you wish to use.
      </p>
    </div>
  </div>
}
