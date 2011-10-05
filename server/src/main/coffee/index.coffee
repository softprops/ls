(($) ->
  $ () ->
    ls = window.ls()
    self = this
    li = (l) ->
      console.log l
      "<li> <a href='https://github.com/#{l.ghuser}/#{l.ghrepo}'>#{l.name}</a> #{l.description} #{l.version}</li>"
    ls.libraries (err, libs) ->
      buf = []
      buf.push(li l) for l in libs
      $("#libraries").append(buf.join(''))
    $("#q").keyup (e) ->
      q = $(this).val()
      console.log q
      if q.length > 3
        ls.any q, (err, libs) ->
          buf = []
          buf.push(li l) for l in libs
          $("#libraries").html(buf.join(''))
          return
    return
  return
)(jQuery)