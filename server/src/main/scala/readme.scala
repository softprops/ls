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
          ls learns about your library through a process called <code>lsyncing</code>, which synchronizes with build information hosted on Github. Because only privileged parties may commit to these repositories, ls is not concerned with who is requesting an lsync—that is, authentication—but with the controlled project metadata on Github.
      </p>
      <h2 id="lsync-spec">Lsync spec format</h2>
      <p>
        ls stores semi-structured information about your library's build in a json encoded file for each published version of your library. It is encouraged to only lsync build info about released versions.
      </p>
      <p>The following is a description of the format that ls uses to capture library information.
      <pre><code>{{
 <span class="comment"># a maven/ivy organization identifier</span>
 <span class="key">"organization"</span>:"org.yourdomain",
 <span class="comment"># name of your library</span>
 <span class="key">"name"</span>:"my-awesome-library",
 <span class="comment"># version of your library</span>
 <span class="key">"version"</span>:"1.0",
 <span class="comment"># quaint description of what it provides</span>
 <span class="key">"description"</span>:"Hot stuff comin' through",
 <span class="comment"># a uri for more info &amp; resources</span>
 <span class="key">"site"</span>:"http://yourdomain.org/awesome-library-overview",
 <span class="comment"># keywords to help others to find your library</span>
 <span class="key">"tags"</span>: ["awesome", "opossum"],
 <span class="comment"># a uri for version-specific api docs</span>
 <span class="key">"docs"</span>:"https://yourdomain.org/awesome-library-docs",
 <span class="comment"># licenses if you care for them</span>
 <span class="key">"licenses"</span>: [{{
   "name": "MIT",
   "url":"https://yourdomain.org/awesome-library/LICENSE"
  }}],
 <span class="comment"># where can your library be downloaded from</span>
 <span class="key">"resolvers"</span>: ["http://repo.yourdomain.org/"],
 <span class="comment"># what does your library depend on</span>
 <span class="key">"dependencies"</span>: [{{
   "organization":"org.otherdomain",
   "name": "my-awesome-dependency",
   "version": "0.1.0"
  }}],
 <span class="comment"># scala cross-binary dialects</span>
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
     This will tell ls to extract any files in the <code>github.com/your-gh-user/your-gh-repo</code> repository for files matching <code>src/main/ls/version-to-sync.json</code> and store the libraries metadata.
      </p>
      <p>
        No wants you to hand copy that for every library you write. That's what plugins are for.
      </p>
      <h2>Say hello to ls-sbt</h2>
      <p>To install a convenient ls client, install the following <a href="https://github.com/n8han/conscript">conscript</a></p>
      <pre><code>$ cs softprops/ls</code></pre>
      <p>Then in the root of your project run</p>
      <pre><code>$ lsinit</code></pre>
      <p>With the plugin installed to your project, you can generate version info automatically from its sbt build.</p>
      <pre><code>sbt> ls-write-version</code></pre>
      <p>Add or edit the generated file under <code>src/main/ls/:version.json</code> to fit your liking before commiting to git and pushing to your Github remote.</p>
      <p>When you are ready to sync your libraries build info with ls, do the following</p>
      <pre><code>sbt> lsync</code></pre>
      <p>That's it! Free free to go ahead and start banging out the next awesome version of your library now.</p>
    </div>

    <div id="finding">
      <h1><a href="#finding">Thumbing through</a></h1>
      <p>There are lot of libraries out there, waiting to make your life easier.</p>
      <p>ls aims to make finding Scala libraries as simple as it possibly can be by adding some vocabulary to your sbt shell.</p>
      <p>To find a library by name use <code>ls -l</code></p>
      <pre><code>sbt> ls -l unfiltered</code></pre>
      <p>This command will find any ls-published library named <code>unfiltered</code>, listing recent versions with descriptions</p>
      <p>What if you are looking for a specific version? Just add <code>@version</code> to your query</p>
      <pre><code>sbt> ls -l unfiltered@0.5.1</code></pre>
      <p>What if you don't know the name of the library you are looking for? Try searcing by tags and see what pops up.</p>
      <pre><code>sbt> ls web netty</code></pre>
      <p>Tags help categorize libraries and make them easier for users to find.</p>
    </div>

    <div id="installing">
       <h1><a href="installing">Checking the fit</a></h1>
       <p>Once you find the library you are looking for, you have a few options. You can hand edit a configuration file, paste in the info you had to previous search for or you could do</p>
       <pre><code>sbt> ls-try unfiltered</code></pre>
       <p>This will temporarily add the latest version of unfiltered to your library chain. Try <code>console-quick</code> to play with the library in the repl. If you don't like it you can always remove it with the sbt built-in command <code>session clear</code> or by reloading your project. If you do find the library that fits you need, it's just as easy to install it.
       </p>
       <pre><code>sbt> ls-install unfiltered</code></pre>
       <p>The same syntax for specifying a specific version applies</p>
       <pre><code>sbt> ls-install unfiltered@0.5.1</code></pre>
    </div>
    <div id="uris">
      <h1><a href="#uris">You or I</a></h1>
      <p>Because of the open nature of Github, a forked repository can coexist with another of the same name under a different user's account. Libraries is ls are just references to <code>uris</code>. More specifically a uri for Github repository, a library name, and version.</p>
      <pre><code>sbt> ls -l library@0.5.0 user/repo</code></pre>
      <p>
        By default <code>try</code> and <code>install</code> will always use the latest version of a library by name using an inferred uri.
      </p>
      <p>
        It is not recommended to lsync version information across repos. Doing so may decrease the likelihood of your users finding the right library. In the case a library author has lsync'd across repositories, ls will prompt you for the repo you wish to use.
      </p>
    </div>
  </div>
}
