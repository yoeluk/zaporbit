"use strict"

# Directives
angular.module "ZapOrbit.directives", []
.directive "appVersion", ["version", (version) ->
  link : (scope, elm, attrs) ->
    elm.text version
]
.directive "reverseGeocode", [ ->
  restrict: "E"
  template: "<div></div>"
  link: (scope, element, attrs) ->
    geocoder = new google.maps.Geocoder()
    latlng = new google.maps.LatLng(attrs.lat, attrs.lng)
    geocoder.geocode
      latLng: latlng
    , (results, status) ->
      if status is google.maps.GeocoderStatus.OK
        if results[1]
          element.text results[1].formatted_address
        else
          element.text "Location not found"
      else
        element.text "Geocoder failed due to: " + status

  replace: true
]
.directive "backImg", [->
  link: (scope, element, attrs) ->
    attrs.$observe "backImg", (value) ->
      element.css
        "background-image": "url(" + value + ")"
        "background-size": "cover"
]
.directive "imgCentred", [->
  link: (scope, element, attrs) ->
    attrs.$observe "imgCentred", (value) ->
      element.css
        "background": "url(" + value + ") repeat-y center center"
        "background-size": "cover"
]
.directive "imgOverride", [->
  link: (scope, element, attrs) ->
    attrs.$observe "imgOverride", (value) ->
      if value != "" then element.css
        "background": "url(" + value + ") repeat-y center center"
        "background-size": "cover"
]
.directive 'infiniteScroll', ['$rootScope', '$window', '$timeout', 'THROTTLE_MILLISECONDS', ($rootScope, $window, $timeout, THROTTLE_MILLISECONDS) ->
  scope:
    infiniteScroll: '&'
    infiniteScrollContainer: '='
    infiniteScrollDistance: '='
    infiniteScrollDisabled: '='
    infiniteScrollUseDocumentBottom: '='

  link: (scope, elem, attrs) ->
    windowElement = angular.element($window)

    scrollDistance = null
    scrollEnabled = null
    checkWhenEnabled = null
    container = null
    immediateCheck = true
    useDocumentBottom = false

    height = (elem) ->
      elem = elem[0] or elem

      if isNaN(elem.offsetHeight) then height(elem.document.documentElement) else elem.offsetHeight

    offsetTop = (elem) ->
      if not elem[0].getBoundingClientRect or elem.css('none')
        return

      elem[0].getBoundingClientRect().top + pageYOffset(elem)

    pageYOffset = (elem) ->
      elem = elem[0] or elem

      if isNaN(window.pageYOffset) then elem.document.documentElement.scrollTop else elem.ownerDocument.defaultView.pageYOffset

    # infinite-scroll specifies a function to call when the window,
    # or some other container specified by infinite-scroll-container,
    # is scrolled within a certain range from the bottom of the
    # document. It is recommended to use infinite-scroll-disabled
    # with a boolean that is set to true when the function is
    # called in order to throttle the function call.
    handler = ->
      if container == windowElement
        containerBottom = height(container) + pageYOffset(container[0].document.documentElement)
        elementBottom = offsetTop(elem) + height(elem)
      else
        containerBottom = height(container)
        containerTopOffset = 0
        if offsetTop(container) != undefined
          containerTopOffset = offsetTop(container)
        elementBottom = offsetTop(elem) - containerTopOffset + height(elem)

      if(useDocumentBottom)
        elementBottom = height((elem[0].ownerDocument || elem[0].document).documentElement)

      remaining = elementBottom - containerBottom
      shouldScroll = remaining <= height(container) * scrollDistance + 1

      if shouldScroll
        checkWhenEnabled = true

        if scrollEnabled
          if scope.$$phase || $rootScope.$$phase
            scope.infiniteScroll()
          else
            scope.$apply(scope.infiniteScroll)
      else
        checkWhenEnabled = false

    # The optional THROTTLE_MILLISECONDS configuration value specifies
    # a minimum time that should elapse between each call to the
    # handler. N.b. the first call the handler will be run
    # immediately, and the final call will always result in the
    # handler being called after the `wait` period elapses.
    # A slimmed down version of underscore's implementation.
    throttle = (func, wait) ->
      timeout = null
      previous = 0
      later = ->
        previous = new Date().getTime()
        $timeout.cancel(timeout)
        timeout = null
        func.call()
        context = null

      return ->
        now = new Date().getTime()
        remaining = wait - (now - previous)
        if remaining <= 0
          clearTimeout timeout
          $timeout.cancel(timeout)
          timeout = null
          previous = now
          func.call()
        else timeout = $timeout(later, remaining) unless timeout

    if THROTTLE_MILLISECONDS?
      handler = throttle(handler, THROTTLE_MILLISECONDS)

    scope.$on '$destroy', ->
      container.off 'scroll', handler

    # infinite-scroll-distance specifies how close to the bottom of the page
    # the window is allowed to be before we trigger a new scroll. The value
    # provided is multiplied by the container height; for example, to load
    # more when the bottom of the page is less than 3 container heights away,
    # specify a value of 3. Defaults to 0.
    handleInfiniteScrollDistance = (v) ->
      scrollDistance = parseFloat(v) or 0

    scope.$watch 'infiniteScrollDistance', handleInfiniteScrollDistance
    # If I don't explicitly call the handler here, tests fail. Don't know why yet.
    handleInfiniteScrollDistance scope.infiniteScrollDistance

    # infinite-scroll-disabled specifies a boolean that will keep the
    # infnite scroll function from being called; this is useful for
    # debouncing or throttling the function call. If an infinite
    # scroll is triggered but this value evaluates to true, then
    # once it switches back to false the infinite scroll function
    # will be triggered again.
    handleInfiniteScrollDisabled = (v) ->
      scrollEnabled = !v
      if scrollEnabled && checkWhenEnabled
        checkWhenEnabled = false
        handler()

    scope.$watch 'infiniteScrollDisabled', handleInfiniteScrollDisabled
    # If I don't explicitly call the handler here, tests fail. Don't know why yet.
    handleInfiniteScrollDisabled scope.infiniteScrollDisabled

    # use the bottom of the document instead of the element's bottom.
    # This useful when the element does not have a height due to its
    # children being absolute positioned.
    handleInfiniteScrollUseDocumentBottom = (v) ->
      useDocumentBottom = v

    scope.$watch 'infiniteScrollUseDocumentBottom', handleInfiniteScrollUseDocumentBottom
    handleInfiniteScrollUseDocumentBottom scope.infiniteScrollUseDocumentBottom

    # infinite-scroll-container sets the container which we want to be
    # infinte scrolled, instead of the whole window. Must be an
    # Angular or jQuery element, or, if jQuery is loaded,
    # a jQuery selector as a string.
    changeContainer = (newContainer) ->
      if container?
        container.off 'scroll', handler

      container = if typeof newContainer.last is 'function' && newContainer != windowElement then newContainer.last() else newContainer
      if newContainer?
        container.on 'scroll', handler

    changeContainer windowElement

    handleInfiniteScrollContainer = (newContainer) ->
      # TODO: For some reason newContainer is sometimes null instead
      # of the empty array, which Angular is supposed to pass when the
      # element is not defined
      # (https://github.com/sroze/ngInfiniteScroll/pull/7#commitcomment-5748431).
      # So I leave both checks.
      if (not newContainer?) or newContainer.length == 0
        return
      newContainer = angular.element document.querySelector newContainer
      if newContainer?
        changeContainer newContainer
      else
        throw new Exception("invalid infinite-scroll-container attribute.")

    scope.$watch 'infiniteScrollContainer', handleInfiniteScrollContainer
    handleInfiniteScrollContainer(scope.infiniteScrollContainer or [])

    # infinite-scroll-parent establishes this element's parent as the
    # container infinitely scrolled instead of the whole window.
    if attrs.infiniteScrollParent?
      changeContainer angular.element elem.parent()

    # infinte-scoll-immediate-check sets whether or not run the
    # expression passed on infinite-scroll for the first time when the
    # directive first loads, before any actual scroll.
    if attrs.infiniteScrollImmediateCheck?
      immediateCheck = scope.$eval(attrs.infiniteScrollImmediateCheck)

    $timeout (->
      if immediateCheck
        handler()
    ), 0
]
.directive "fbLogin", ["$rootScope", "$timeout", ($rootScope, $timeout) ->
  link: (scope, iElement, iAttrs) ->
    reparse = (e) ->
      if FB? then FB.XFBML.parse e[0]
    $timeout ->
      reparse(iElement)
]
.directive "scrollTop", ["$window", ($window) ->
  link: (scope, element, attrs) ->
    angular.element($window).bind "scroll", ->
      if @pageYOffset >= 55
        scope.fixedToTop = true
        scope.userHome = true
      else
        scope.fixedToTop = false
        scope.userHome = false
      scope.$apply()
]
.directive "replyAttr", [ ->
  restrict: "A"
  require: "?ngModel"
  link: (scope, element, attrs, ngModel) ->
    if !ngModel then return
    read = ->
      ngModel.$setViewValue element.text()
      if scope.replies[scope.activePill[scope.activeTab]]?
        scope.replies[scope.activePill[scope.activeTab]] = element.text()

    ngModel.$render = ->
      element.text ngModel.$viewValue or ""

    element.bind "blur keyup change", ->
      scope.$apply read

    scope.$on "replied", (e, index) ->
      if index == parseInt(attrs.index)
        ngModel.$setViewValue ""
        ngModel.$render()
]
.directive "myDescription", ["$timeout", ($timeout) ->
  restrict: "A"
  link: (scope, element, attrs) ->

    myTrim = (x) ->
      x.replace(/^\s+|\s+$/gm,'')

    initMessage = "Tell others a little bit about you in one sentence. What is worth your while?"

    element.bind "focus", (e) ->
      if myTrim( element.text() ) == initMessage then element.empty()

    element.bind "blur", (e) ->
      if myTrim( element.text() ) == "" then element.text(initMessage)

    element.bind "keydown", (e) ->
      key = if e.keyCode == 13 then "Enter" else if e.keyCode == 8 then "Backspace"
      if key == "Enter" then e.preventDefault()
      if key != "Backspace" && element.text().length == parseInt(attrs.max)
        e.preventDefault()
      else
        $timeout ->
          trimmedAboutMe = myTrim( element.text() )
          if trimmedAboutMe != ""
            scope.$broadcast "aboutMe", trimmedAboutMe
          else scope.$broadcast "aboutMe", initMessage
]
.directive "focus", [ ->
  link: (scope, element, attrs) ->

    scope.$on "activate", (e) ->
      element.get(0).focus()
]
.directive 'disableNgAnimate', ['$animate', ($animate)->
  restrict: 'A'
  link: (scope, element)-> $animate.enabled false, element
]
.directive "sortEvents", ["$timeout", ($timeout) ->
  restrict: "A"
  link: (scope, element, attrs) ->

    element.bind "mousedown", (e) ->
      if e.target.tagName != 'BUTTON'
        element.parent().css( "outline" : "1px dashed #555555" )
        element.css( "background-color" : "rgba(255, 255, 255, 0)" )
        element.find('button').css( "display" : "none" )

    element.bind "mouseup", (e) ->
      if e.target.tagName != 'BUTTON'
        element.parent().css( "outline" : "none" )
        element.find('button').css( "display" : "block" )
        element.css( "background-color" : "rgba(255, 255, 255, 0.3)" )
]
.directive "localeAttr", [ ->
  link: (scope, element, attrs) ->

    element.bind "change", (e) ->
      element.children().each (i) ->
        if $(this).attr('value') == element.val()
          locale = $(this).attr('data-locale')
          scope.$broadcast "localeOpts",
            locale: locale
            currency_code: element.val()
]
.directive "deleteListener", ["$document", "$timeout", ($document, $timeout) ->
  link: (scope, element, attrs) ->

    bodyClickCallback = (e) ->
      element.removeClass "confirm"
      element.data 'delete-listener', false
      angular.element($document[0].body).unbind "click", bodyClickCallback
      if angular.element(e.target).is(element)
        scope.$emit element.data('event'), element.data 'index'

    clickCallback = (e) ->
      if !element.data('delete-listener')
        element.data 'delete-listener', true
        element.addClass "confirm"
        $timeout ->
          angular.element($document[0].body).bind "click", bodyClickCallback
        , 50

    element.bind "click", clickCallback
]
.directive "paypalAuth", ["$document", "$timeout", ($document, $timeout) ->
  link: (scope, element, attrs) ->

    bodyClickCallback = (e) ->
      element.removeClass 'confirm'
      angular.element($document[0].body).unbind "click", bodyClickCallback
      if angular.element(e.target).is(element)
        element.addClass 'disable'
        scope.$broadcast element.data('destructive-event'), element

    clickCallback = (e) ->
      if !element.data('paypal-auth') or element.data('paypal-auth') == "false"
        element.addClass 'disable'
        scope.$broadcast element.data('event'), element
      else if !element.hasClass('confirm')
        element.addClass 'confirm'
        $timeout ->
          angular.element($document[0].body).bind "click", bodyClickCallback
        , 50

    element.bind "click", clickCallback
]
.directive "unfollowEvent", [ ->
  link: (scope, element, attrs) ->

    mouseleaveCallback = (e) ->
      element.removeClass "confirm"
      element.data 'unfollow-event', false

    clickCallback = (e) ->
      if !element.data('unfollow-event')
        element.data 'unfollow-event', true
        element.addClass "confirm"
      else
        scope.$emit 'unfollowEvent', element.data 'index'

    element.bind "click", clickCallback

    element.bind "mouseleave", mouseleaveCallback
]
.directive "htmlAttrDescription", [ ->
  restrict: "A"
  link: (scope, element, attrs) -> scope.$broadcast 'description', element.data 'html-attr-description'
]
.directive 'bgHolder', [ ->
  link: (scope, element, attrs) ->
    element.css 'background' : "url("+attrs.bgHolder+")" + " no-repeat 50% 50%"
    Holder.run images: element[0]
]
.directive "divBlurred", [ ->
  link: (scope, element, attrs) ->
    element.blurjs
      source: '.header-view'
      overlay: 'rgba(0,100,100,0.1)'
]
.directive "starterMessage", ["$filter", ($filter) ->
  restrict: "A"
  require: "ngModel"
  link: (scope, element, attrs, ngModel) ->

    scope.$on "starterMsgSent", (e) ->
      element.text attrs.placeholder

    read = ->
      ngModel.$setViewValue element.text()

    focussed = ->
      trimmed = $filter('trim')(element.text())
      if trimmed? and trimmed == attrs.placeholder
        element.text ""

    blurred = ->
      trimmed = $filter('trim')(element.text())
      if trimmed? and trimmed == ""
        element.text attrs.placeholder

    ngModel.$render = ->
      trimmed = $filter('trim')(element.text())
      if trimmed? and trimmed == ""
        element.text attrs.placeholder
      else
        element.text ngModel.$viewValue or ""

    element.bind "blur keyup change", ->
      scope.$apply read

    element.bind "blur", ->
      scope.$apply blurred

    element.bind "focus", ->
      scope.$apply focussed
]
.directive "emHeight", ["$timeout", ($timeout) ->
  link: (scope, $el, attrs) ->

    elem = $el[0]

    scope.$watch (scope) ->
      elem.scrollHeight
    , (newScrollHeight) ->
      $timeout ->
        scope.$apply ->
          elem.scrollTop = elem.scrollHeight
]