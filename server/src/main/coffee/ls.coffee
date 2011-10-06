root = (exports ? window)
$ = jQuery
$ ->
  # ls api client
  root.ls = () ->
    api = 'http://' + window.location.host + "/api"
    {
      # return a list of libraries
      libraries: (f) ->
        $.get api + "/libraries", (libs) ->
          f null, libs
      # any search api by query term(s) in q
      any: (q, f) ->
        $.get api + "/any", q: q, (libs) ->
          f null, libs
    }