root = (exports ? window)
$ = jQuery
$ ->
  # ls api client
  root.ls = () ->
    api = 'http://' + window.location.host + "/api"
    fallback = (f) -> (e) -> f []
    methods = {
      # return a list of all libraries
      libraries: (f) ->
        ($.get "#{api}/libraries",(libs) -> f libs).error(fallback f)
      # any search api by query term(s) in q
      search: (q, f) ->
        ($.get "#{api}/search", q: q, (libs) -> f libs).error(fallback f)
      # list of libraries associated with a project
      projects: (u, r, f) ->
        ($.get "${api}/repositories/#{u}/#{r}", (libs) -> f libs).error(fallback f)
      # search by author name
      authors: (a, f) ->
        ($.get "#{api}/authors/#{a}", (libs) -> f libs).error(fallback f)
    }
    methods