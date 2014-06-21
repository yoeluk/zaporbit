"use strict"

# Filters
angular.module("ZapOrbit.filters", []

).filter("interpolate", ["version", (version) ->

    (text) ->
      String(text).replace /\%VERSION\%/g, version

  ])