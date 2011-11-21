$ = jQuery
$ ->
  $('ul.version-nums li').hover (e) ->
    li = $(this)
    ul = $(li.parent())
    ul.find('li').removeClass('sel')
    li.addClass('sel')
    id = li.attr('id')
    $(ul.parent()).parent().find('div.version-details div').removeClass('sel');
    $('#v-' + id).addClass('sel')
  $('ul.version-nums li a').click (e) ->
    e.preventDefault
    false
  $('.clippy').clippy(color: "#eeeeee")