'use strict'
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
.constant "pageSize", 25
.config ["$locationProvider", "$routeProvider", "$httpProvider", "$provide", ($locationProvider, $routeProvider, $httpProvider, $provide) ->
  $locationProvider
    .html5Mode false
    .hashPrefix "!"
  $httpProvider
    .interceptors.push "SessionInjector"
  $routeProvider
    .when "/",
      templateUrl: "/partials/listings"
      controller: "ShoppingCtrl"
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
      reloadOnSearch: false
    .when "/userprofile",
      templateUrl: (params) -> "/userprofile?id="+params.id+"&rnd="+params.rnd
      controller: "UserProfileCtrl"
      reloadOnSearch: false
    .otherwise "/listings"
  $provide.decorator "taOptions", ["$delegate", (taOptions) ->
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
          "clear"
        ]
        [
          "justifyLeft"
          "justifyCenter"
          "justifyRight"
        ]
        [
          "html"
          "insertLink"
          "insertImage"
          "insertVideo"
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
    taRegisterTool "colourRed",
      iconclass: "fa fa-square red"
      action: ->
        @$editor().wrapSelection "forecolor", "red"

    # add the button to the default toolbar definition
    taOptions.toolbar[1].push "colourRed"
    return taOptions
  ]
#  GoogleMapApi.configure
#    key: 'AIzaSyDWU1SnyZt9QxEqXgLoeHylM4RnKFeYPbI'
#    v: '3.17',
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
.animation "animate", ->
  enter: (element, done) ->
    element.css "opacity", 0
    console.log 'entering'
    jQuery(element).animate
      opacity: 1
    , done

    # optional onDone or onCancel callback
    # function to handle any post-animation
    # cleanup operations
    (isCancelled) ->
      jQuery(element).stop()  if isCancelled
      return

  leave: (element, done) ->
    element.css "opacity", 1
    jQuery(element).animate
      opacity: 0
    , done

    # optional onDone or onCancel callback
    # function to handle any post-animation
    # cleanup operations
    (isCancelled) ->
      jQuery(element).stop()  if isCancelled
      return

  move: (element, done) ->
    element.css "opacity", 0
    jQuery(element).animate
      opacity: 1
    , done

    # optional onDone or onCancel callback
    # function to handle any post-animation
    # cleanup operations
    (isCancelled) ->
      jQuery(element).stop()  if isCancelled
      return


  # you can also capture these animation events
  addClass: (element, className, done) ->

  removeClass: (element, className, done) ->
