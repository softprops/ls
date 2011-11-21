root = (exports ? window)
$ = jQuery
$ ->
  # ls api client
  root.ls = () ->
    api = 'http://' + window.location.host + "/api/1"
    fallback = (f) -> (e) -> f []
    methods = {
      # return a list of all libraries
      libraries: (pg, lim, f) ->
        ($.get "#{api}/libraries", page: pg, limit: lim, (libs) -> f libs).error(fallback f)
      # any search api by query term(s) in q
      search: (q, pg, lim, f) ->
        ($.get "#{api}/search", q: q, page: pg, limit: lim, (libs) -> f libs).error(fallback f)
      # list of libraries associated with a project
      projects: (u, r, f) ->
        ($.get "${api}/repositories/#{u}/#{r}", (libs) -> f libs).error(fallback f)
      # search by author name
      authors: (a, f) ->
        ($.get "#{api}/authors/#{a}", (libs) -> f libs).error(fallback f)
    }
    methods