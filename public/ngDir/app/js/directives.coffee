"use strict"

# Directives
angular.module("ZapOrbit.directives", [

]).directive("appVersion", ["version", (version) ->
  link : (scope, elm, attrs) ->
    elm.text version
])