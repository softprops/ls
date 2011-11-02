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
    "<li>
      <h3>
        <a href='/#{l.ghuser}/#{l.ghrepo}\##{l.name}'>#{l.name}<span class='at'>@</span><span class='v'>#{l.versions[0].version}</span></a>
      </h3>
      <div>#{l.description}</div>
     </li>"

  perPage = 9

  display = (page) ->
    (libs) ->
      $("#libraries").removeClass("spin")
      if libs.length
        visible = libs.slice(0, perPage)
        [c, r] = [2, 3]
        columns = [0..c].map (i) ->
          flatten ['<ul>', (visible[i*r...i*r+r].map (l) -> li l), '</ul>']
        rows = flatten(columns)
        rows.push('<div class="pagination">')
        if page > 1
          rows.push("<a href='javascript:void(0)' class='page' data-page='#{page-1}'>less</a>")
        if libs.length > perPage
          rows.push("<a href='javascript:void(0)' class='page' data-page='#{page+1}'>more</a>")
        rows.push("</div>")
        $("#libraries").html(rows.join(''))
      else
        $("#libraries").html(
          "<div class='none-found'>No published libraries found. You should start one.</div>"
          )

  $("a.page").live 'click', (e) ->
    e.preventDefault()
    pg = $(this).data().page
    ls.libraries pg, perPage+1, display(pg)
    return false

  $("#libraries").addClass("spin")

  if window.location.hash.length and not window.location.hash in ['#publishing', '#finding', '#installing', '#uris']
    term = window.location.hash.substring(1)
    $("#q").val(term)
    ls.search term, 1, perPage+1, display(1)
  else
    ls.libraries 1, perPage+1, display(1)

  # search box
  # we don't want to issue a query after every since key up event
  # instead, only issue a query when the user appears to be done typing
  # if this gets any more complex, consider using a plugin
  typeTimeout = null

  search = () ->
    q = $.trim($("#q").val())
    $("#libraries").empty().addClass("spin")
    if q.length > 2
      ls.search q, 1, perPage+1, display(1)
    else if q.length is 0
      ls.libraries 1, perPage+1, display(1)

  $("#q").keyup (e) ->
    typeTimeout = setTimeout(search, 700)

  $("#q").keydown (e) ->
    clearTimeout(typeTimeout)
