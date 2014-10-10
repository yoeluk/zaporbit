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
.filter "pictureName", [ ->
  (size) ->
    name = "";
    chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@*_$";
    i = 0
    while i < size
      name += chars.charAt(Math.floor(Math.random() * chars.length));
      ++i
    name
]
.filter "appendExt", [ ->
  (name) ->
    parts = name.split "."
    return name if parts.length > 1
    return name + ".jpg"
]
.filter "formatPrice", [ ->
  (listing) ->
    if listing.currency_code == "TRY"
      listing.price + " &#xf195;"
    else if listing.currency_code == "RUB"
      listing.price + " &#xf158;"
    else listing.formatted_price
]