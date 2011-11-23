jQuery ->
  if window.history? and history.pushState?
    $('a.lib-link').live 'click', (e) ->
      e.preventDefault()
      history.pushState null, null, @.href
    $(window).bind 'popstate', (e) ->
      console.log location.pathname
      console.log e
      if location.pathname is '/' then console.log 'home'
      else
        parts = location.pathname.split('/').splice(1)
        if parts.length is 1 then console.log 'user'
        else if pathparts.length is 2 then console.log 'project'

