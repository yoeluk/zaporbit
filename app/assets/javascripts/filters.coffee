"use strict"

# Filters
angular.module "ZapOrbit.filters", []
.filter "interpolate", ["version", (version) ->

    (text) ->
      String(text).replace /\%VERSION\%/g, version
]
.filter "reverse", [ ->
  (items) ->
    items.slice().reverse()
]
.filter "currencys", [
  "$filter"
  "$locale"
  ($filter, $locale) ->
    return (num) ->
      sym = $locale.NUMBER_FORMATS.CURRENCY_SYM
      $filter("currency") num, "<span>" + sym + "</span>"
]
.filter "trim", [ ->
  (x) ->
    x.replace(/^\s+|\s+$/gm,'')
]
.filter "isNumber", [ ->
  (n) ->
    ( !isNaN(parseInt(n)) || !isNaN(parseFloat(n)) ) && isFinite(n)
]