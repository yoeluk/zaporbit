'use strict'

# Declare app level module which depends on filters, and services
angular.module "ZapOrbit", [
  "ngRoute",
  "ngResource",
  "ngAnimate",
  "ZapOrbit.filters",
  "ZapOrbit.services",
  "ZapOrbit.directives",
  "ZapOrbit.controllers",
  "ui.bootstrap",
  "google-maps",
  "angularMoment",
  "LocalStorageModule",
  'angularFileUpload',
  'ui.sortable',
  'textAngular'
]
.run ["$rootScope", ($rootScope) ->

]
.constant "pageSize", 25
.config ["$locationProvider", "$routeProvider", "$httpProvider", "$provide", ($locationProvider, $routeProvider, $httpProvider, $provide) ->
  $locationProvider
    .html5Mode false
    .hashPrefix "!"
  $httpProvider
    .interceptors.push "SessionInjector"
  $routeProvider
    .when "/",
      templateUrl: "/partials/home"
      controller: "HomeCtr"
    .when "/listings",
      templateUrl: "/partials/listings"
      controller: "ShoppingCtrl"
    .when "/support",
      templateUrl: "/partials/support"
      controller: "SupportCtrl"
    .when "/listing_item/:itemid",
      templateUrl: (params) -> "listing_item/" + params.itemid
      controller: "ListingCtrl"
    .when "/userhome",
      templateUrl: "/partials/userhome"
      controller: "UserHomeCtrl"
    .when "/userprofile",
      templateUrl: (params) -> "/userprofile?id="+params.id
      controller: "UserProfileCtrl"

  $provide.decorator "taOptions", ["$delegate", (taOptions) ->
      # $delegate is the taOptions we are decorating
      # here we override the default toolbars and classes specified in taOptions.
      taOptions.toolbar = [
        [
          "h1"
          "h2"
          "h3"
          "quote"
        ]
        [
          "bold"
          "italics"
          "underline"
          "ul"
          "ol"
          "redo"
          "undo"
        ]
        [
          "justifyLeft"
          "justifyCenter"
          "justifyRight"
        ]
        [
          "html"
          "insertImage"
          "insertLink"
          "unlink"
        ]
      ]
      taOptions.classes =
        focussed: "focussed"
        toolbar: "btn-toolbar"
        toolbarGroup: "btn-group"
        toolbarButton: "btn btn-default"
        toolbarButtonActive: "active"
        disabled: "disabled"
        textEditor: "form-control"
        htmlEditor: "form-control"
      return taOptions
  ]

  $provide.decorator "taOptions", ["taRegisterTool", "$delegate", (taRegisterTool, taOptions) ->
    # $delegate is the taOptions we are decorating
    # register the tool with textAngular
    taRegisterTool "colourRed",
      iconclass: "fa fa-square red"
      action: ->
        @$editor().wrapSelection "forecolor", "red"

    # add the button to the default toolbar definition
    taOptions.toolbar[1].push "colourRed"
    return taOptions
  ]
]
angular.module 'infinite-scroll', []
.directive 'infiniteScroll', ['$rootScope', '$window', '$timeout', ($rootScope, $window, $timeout) ->

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
]
