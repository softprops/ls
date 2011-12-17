###
Renders a 3 column list of recently published libraries
and binds to search queries to api
###
root = exports ? window
$ = jQuery
$ ->
  ls = root.ls()
  self = this
  flatten = (arys) ->
    buf = (buf ?= []).concat(a) for a in arys
    buf

  li = (l) ->
    "<li class='lib-small-info'>
      <h3>
        <a class='lib-link' href='/#{l.ghuser}/#{l.ghrepo}\##{l.name}'>#{l.name}<span class='at'>@</span><span class='v'>#{l.versions[0].version}</span></a>
      </h3>
      <div>#{l.description}</div>
      <div class='t'>#{Time.agoInWords(l.updated)}</div>
     </li>"

  $('li.lib-small-info').live 'click', (e) ->
    $(location).attr 'href', $(e.currentTarget).find('a').attr('href')

  perPage = 9

  display = (page, term) ->
    (libs) ->
      content = $("#libraries .content").removeClass "spin"
      if libs.length
        visible = libs.slice 0, perPage
        [c, r] = [2, 3]
        columns = [0..c].map (i) ->
          flatten ['<ul>', (visible[i*r...i*r+r].map (l) -> li l), '</ul>']
        rows = flatten(columns)
        pagination = ['<div class="pagination">']
        if page > 1
          pagination.push("<a href='javascript:void(0)' class='page' data-term='#{term}' data-page='#{page-1}'>less</a>")
        if (libs.length > perPage and page is 1) or (libs.length is perPage + 2 and page > 1)
          pagination.push("<a href='javascript:void(0)' class='page' data-term='#{term}' data-page='#{page+1}'>more</a>")
        pagination.push("</div>")
        newrows = $("<div data-page='#{page}' class='clearfix'>#{rows.join('')}</div>")
        content.html(newrows)

        $("#libraries .control").html pagination.join('')
      else
        why = if term then "matching <span class='term'>#{term}</span>" else "found"
        $("#libraries .content").html(
          "<div class='none-found'>No published libraries #{why}. Maybe you should start one.</div>"
          )
        $("#libraries .control").empty()

  # pagination
  $("a.page").live 'click', (e) ->
    e.preventDefault()
    link = $(this).data()
    pg = link.page
    term = link.term
    $("#libraries .content").empty().addClass "spin"
    if term is "undefined"
      ls.libraries pg, perPage+1, display(pg)
    else
      ls.search term, pg, perPage + 1, display(pg, term)
    false

  # bootstrap search if fragment is present
  if window.location.hash.length and window.location.hash not in ['#publishing', '#finding', '#installing', '#uris', '#ls-issues']
    term = window.location.hash.substring(1)
    $("#q").val(term)
    $('#libraries').show 'slow'
    ls.search term, 1, perPage+1, display(1, term)

  # search box
  # we don't want to issue a query after every key up event.
  # instead, only issue a query when the user appears to be done typing.
  # if this gets any more complex, consider using a plugin
  typeTimeout = null

  search = () ->
    q = $.trim $("#q").val()
    if q.length > 2
      $("#libraries .content").empty().addClass "spin"
      ls.search q, 1, perPage+1, display(1, q)
    else if q.length is 0
      $("#libraries .content").empty().addClass "spin"
      ls.libraries 1, perPage+1, display(1)

  $("#q").keyup (e) ->
    typeTimeout = setTimeout search, 500

  $("#q").keydown (e) ->
    clearTimeout typeTimeout

  $("#q-form").live 'submit', (e) ->
    e.preventDefault()
    clearTimeout typeTimeout
    search
    false

  $("#q").focus (e) ->
    $("#libraries").show 'slow', (e) ->
      if $(window).width() < 940
        $('#index').removeClass 'block'
      if $("#q").val().replace(/\s+/g,'').length < 1
        help = $('<h1 id="find-help">What can I help you find?</h1>').hide()
        $('#libraries .content').html help
        $("#libraries .control").empty()
        help.fadeIn 'slow'

  # vertical centering comes at a cost
  layout = () ->
     if $(window).width() < 940
        $('#index').removeClass 'block'
        $('#container').css 'width': '940px'
  $(window).bind('resize', layout)
