root = (exports ? this)
(($) ->
  root.ls = () ->
    api = 'http://' + window.location.host + "/api"
    {
      libraries: (f) ->
        $.get api + "/libraries", (libs) ->
          f null, libs
      any: (q, f) ->
        $.get api + "/any", q: q, (libs) ->
          f null, libs
    }
  return
)(jQuery)
