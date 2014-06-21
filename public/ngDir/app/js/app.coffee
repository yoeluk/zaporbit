'use strict'

# Declare app level module which depends on filters, and services
angular.module("ZapOrbit", [
  "ngRoute",
  "ZapOrbit.filters",
  "ZapOrbit.services",
  "ZapOrbit.directives",
  "ZapOrbit.controllers",
  "infinite-scroll",
  "ui.bootstrap"
])
.constant("apiUrl", "http://youtrack.zaporbit.com/res/")
.constant("ngUrl", "http://localhost:9000/assets/ngDir/app/partials/")
.config ["$routeProvider", "ngUrl", ($routeProvider, ngUrl) ->
  $routeProvider
  .when "/",
    templateUrl: ngUrl + "home.html"
    controller: "HomeCtr"
  .when "/about",
    templateUrl: ngUrl + "about.html"
    controller: "AboutCtrl"
  .when "/support",
    templateUrl: ngUrl + "support.html"
    controller: "SupportCtrl"
]

angular.module('infinite-scroll', [])
.directive('infiniteScroll', ['$rootScope', '$window', '$timeout', ($rootScope, $window, $timeout) ->

    link: (scope, elem, attrs) ->

      $window = angular.element($window)

      scrollDistance = 0
      if attrs.infiniteScrollDistance?
        scope.$watch attrs.infiniteScrollDistance, (value) ->
          scrollDistance = parseInt(value, 10)

      scrollEnabled = true
      checkWhenEnabled = false
      if attrs.infiniteScrollDisabled?
        scope.$watch attrs.infiniteScrollDisabled, (value) ->
          scrollEnabled = !value
          if scrollEnabled && checkWhenEnabled
            checkWhenEnabled = false
            handler()

      handler = ->
        if elem.is(":visible")
          windowBottom = $window.height() + $window.scrollTop()
          elementBottom = elem.offset().top + elem.height()
          remaining = elementBottom - windowBottom
          shouldScroll = remaining <= $window.height() * scrollDistance

        if shouldScroll && scrollEnabled
          if $rootScope.$$phase
            scope.$eval attrs.infiniteScroll
          else
            scope.$apply attrs.infiniteScroll
        else if shouldScroll
          checkWhenEnabled = true

      $window.on 'scroll', handler
      scope.$on '$destroy', ->
        $window.off 'scroll', handler

      $timeout (->
        if attrs.infiniteScrollImmediateCheck
          if scope.$eval(attrs.infiniteScrollImmediateCheck)
            handler()
        else
          handler()
      ), 0
  ])