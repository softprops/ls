window.Time =
  # takes from (in epoc time)
  # and to in Date form
  agoInWords: (from, to = new Date()) ->
    from = new Date(from)
    secs = (to - from) / 1000
    mins = (secs / 60)+ .5 | 0
    if mins is 0 then "just now"
    else if mins is 1 then "a minute ago"
    else if mins < 45 then "#{mins} minutes ago"
    else if mins < 90 then "about an hour ago"
    else if mins < 1440 then "about #{(mins/60)+.5 | 0} hours ago"
    else if mins < 2880 then "yesterday"
    else if mins < 43200 then "#{(mins/1440)+.5 | 0} days ago"
    else if mins < 86400 then "a month ago"
    else if mins < 525960 then "#{(mins/43200)+.5 | 0} months ago"
    else if mins < 1051199 then "a year ago"
    else "over #{(mins/525960) + .5 | 0} years ago"