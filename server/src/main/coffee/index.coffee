root = exports ? window
$ = jQuery
$ ->
  ls = root.ls()
  self = this
  li = (l) ->
    "<li> <a href='https://github.com/#{l.ghuser}/#{l.ghrepo}'>#{l.name}</a><span class='at'>@</span><span class='version'>#{l.version}</span> #{l.description}</li>"
  ls.libraries (err, libs) ->
    buf = []
    buf.push(li l) for l in libs
    $("#libraries").append(buf.join(''))

  # search box
  $("#q").keyup (e) ->
    q = $(this).val()
    if q.length > 3
      ls.any q, do (err, libs) ->
        buf = []
        buf.push(li l) for l in libs
        $("#libraries").html(buf.join(''))
