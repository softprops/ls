# authors coffee
root = exports ? window
$ = jQuery
$ ->
  ls = root.ls()
  self = this
  li = (l) ->
    "<li> <a href='/#{l.ghuser}/#{l.ghrepo}\##{l.name}'>#{l.name}</a><span class='at'>@</span><span class='version'>#{l.version}</span><div> #{l.description}</div></li>"
  ls.authors $("div.author h1").text().replace(/\s+/, ''), (libs) ->
    buf = []
    buf.push(li l) for l in libs
    $("#libraries").append(buf.join(''))