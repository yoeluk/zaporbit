"use strict"

# Controllers
angular.module "ZapOrbit.controllers", ["ngResource"]
.controller "AppCtrl", ["$scope", "$location", ($scope, $location) ->
    $scope.go = (path) ->
      $location.path path
]
.controller "HomeCtr", ["$scope", "$http", ($scope, $http) ->
    $scope.message = "Entice with higher confidence!"
    $scope.motivation = "Free App, lots of possibilities!"
]
.controller "ShoppingCtrl", ["$timeout", "$scope", "LocationService", "ReverseGeocode", "ListingService", "pageSize", "$cookieStore", "localStorageService"
    ($timeout, $scope, LocationService, ReverseGeocode, ListingService, pageSize, $cookieStore, localStorageService) ->

      $scope.getRatingWidth = (user) ->
        'width': 100*((50+user.rating)/(5*(user.ratingCount+10)))+"%"

      $scope.locProg = false
      $scope.locProgMessage = "Discovering your location."
      $scope.allListings = undefined
      $scope.pageSize = pageSize
      $scope.pageRange = ->
        new Array(Math.ceil $scope.paging.total/5)
      $scope.indexes = ->
        Math.floor $scope.paging.total/5/25

      $scope.alerts = []

      $scope.addAlert = (msg) ->
        $scope.alerts.push
          type: "warning"
          msg: msg

      $scope.closeAlert = (index) ->
        $scope.alerts.splice index, 1

      browserLoc = undefined
      if localStorageService.get('loc')? then browserLoc = localStorageService.get 'loc'

      setLocation = (latlng) ->
        if $scope.map.control.getGMap? && $scope.map.control.getGMap()?
          $scope.loc.coords = latlng
          $scope.map.control.getGMap().setZoom 13
          $scope.map.control.refresh
            latitude: latlng.latitude
            longitude: latlng.longitude
        else
          $timeout ->
            setLocation latlng
          , timeInMs

      geocodeCallback = (addr, latlng) ->
        setLocation latlng
        addressCallback addr, undefined, true

      $scope.locate = (form) ->
        $scope.submitted = true
        return if form.$invalid
        $scope.inProgress = true
        $scope.inProgress = false
        $scope.query =
          address: $scope.city + ", " + $scope.region
        ReverseGeocode.geocodeAddress 1, 1, geocodeCallback, $scope.query

      zoom = 10
      timeInMs = 500

      $scope.showSearch = !navigator.geolocation

      listingCallback = (listings) ->
        $scope.allListings = listings
        $scope.paging = ListingService.paging()
        $scope.filterStr = ListingService.filter()
        if !$scope.filterStr && listings.length == 0 && $scope.alerts.length == 0
          $scope.addAlert "There are not listings in this location."

      zoLocation = (addr) ->
        localStorageService.set 'loc'
        ,
          s: addr.street
          c: addr.locality
          r: addr.administrativeArea
          lt: $scope.coords.latitude
          ln: $scope.coords.longitude
        $scope.city = addr.locality
        $scope.region = addr.administrativeArea
        location:
          street: addr.street
          locality: addr.locality
          administrativeArea: addr.administrativeArea
          latitude: $scope.coords.latitude
          longitude: $scope.coords.longitude

      addressCallback = (loc, dummyParam, remote) ->
        $scope.locProg = false
        if loc? && loc.locality?
          ListingService.listings zoLocation(loc), listingCallback, remote

      $scope.coords =
        latitude: 37.77
        longitude: -122.22

      $scope.loc =
        id: 0
        control: {}
        options:
          visible: false
          title: "Your Location"
          draggable: false
        coords:
          latitude: $scope.coords.latitude
          longitude: $scope.coords.longitude

      initLocation = (loc) ->
        $scope.coords =
          latitude: loc.latitude
          longitude: loc.longitude
        $scope.loc.options.visible = true
        $scope.loc.coords =
          latitude: $scope.coords.latitude
          longitude: $scope.coords.longitude
        zoom = 13

      if LocationService.coords()? then initLocation LocationService.coords()
      else if ReverseGeocode.coords()? then initLocation ReverseGeocode.coords()

      if browserLoc && !LocationService.coords() && !ReverseGeocode.coords()
        $scope.city = browserLoc.c
        $scope.region = browserLoc.r
        initLocation
          latitude: browserLoc.lt
          longitude: browserLoc.ln
        zoom = 13
        ListingService.listings
          location:
            street: browserLoc.s
            locality: browserLoc.c
            administrativeArea: browserLoc.r
            latitude: $scope.coords.latitude
            longitude: $scope.coords.longitude
        , listingCallback
        , true

      $scope.map =
        center: $scope.coords
        zoom: zoom
        control: {}
        panTo: false
        options:
          streetViewControl: false
          panControl: false
          maxZoom: 20
          minZoom: 3

      doAfterMapIsLoaded = (task, args) ->
        if $scope.map.control.getGMap?
          if args? then task(args) else task()
        else
          $timeout ->
            doAfterMapIsLoaded task, args
          , timeInMs

      $scope.listingsForLocation = (remote, filter, page)->
        addr = `ReverseGeocode.address() ? zoLocation(ReverseGeocode.address()) : browserLoc ? {
          location: {
            street: browserLoc.s,
            locality: browserLoc.c,
            administrativeArea: browserLoc.r,
            latitude: $scope.coords.latitude,
            longitude: $scope.coords.longitude
            }
          } : void 0`
        if addr?
          ListingService.listings addr, listingCallback, remote, filter, page

      if ReverseGeocode.address()?
        $scope.listingsForLocation false

      displayError = (error) ->
        errors =
          1: 'Permission denied'
          2: 'Position unavailable'
          3: 'Request timeout'
        $scope.showSearch = true
        $scope.locProg = false

      showLocation = (latlng) ->
        if $scope.map.control.getGMap?
          $scope.loc.coords = $scope.coords = latlng
          $scope.loc.options.visible = true
          $scope.map.control.getGMap().setZoom 13
          $scope.map.control.refresh
            latitude: latlng.latitude
            longitude: latlng.longitude
          $scope.locProgMessage = "Validating address."
          ReverseGeocode.geocodeAddress latlng.latitude, latlng.longitude, geocodeCallback
        else
          $timeout ->
            showLocation(latlng)
          , timeInMs

      if navigator.geolocation && !LocationService.coords()
        LocationService.location showLocation, displayError, $scope
]
.controller "SearchCtrl", ["$scope", "ListingService", ($scope, ListingService) ->

    if ListingService.filter()?
      $scope.filterString = ListingService.filter()

    $scope.filter = (form) ->
      if $scope.filterString? && $scope.filterString.replace(/(^\s+|\s+$)/g, '') != ""
        console.log $scope.filterString
        $scope.$parent.listingsForLocation(true, $scope.filterString.replace(/(^\s+|\s+$)/g, ''))
      else
        $scope.$parent.listingsForLocation(true)
]
.controller "SupportCtrl", ["$scope", "$http", ($scope, $http) ->

    $scope.allIssues = []
    $scope.oneAtATime = false
    $scope.isopen = []

    $scope.bugs = 0
    $scope.tasks = 0
    $scope.features = 0
    $scope.ioss = 0
    $scope.webs = 0

    getStats = ->
      $http
        method: "GET"
        url: "api/youtrack/getstats"
        context: this
      .success (data, status) ->
        if status == 200
          count = data["count"]
          $scope.bugs = count[0]
          $scope.tasks = count[1]
          $scope.features = count[2]
          $scope.ioss = count[3]
          $scope.webs = count[4]
          $scope.pipeline = true

    $scope.getIssues = getIssues = ->
      $http
        method: "GET"
        url: "api/youtrack/allissues"
        context: this
      .success (data, status) ->
        if status == 200
          $scope.allIssues = data["issues"]["issue"]
          i = $scope.allIssues.length - 1
          while i > -1
            issue = $scope.allIssues[i]
            issue.props = {}
            ii = 0
            ll = issue["field"].length
            while ii < ll
              prop =  issue["field"][ii]["name"]
              val = issue["field"][ii]["value"]
              issue.props[prop] = val
              ++ii
            $scope.isopen.push (issue.props.State[0] != 'Completed' && issue.props.State[0] != "Fixed")
            i--
          getStats()
      .error (data, status, headers, config) ->

    getIssues()
]
.controller "HeaderController", ["$scope", "$location", ($scope, $location) ->

    $scope.isActive = (viewLocation) ->
      return viewLocation == $location.path()
]
.controller "ModalIssueCtrl", ["$scope", "$modal", ($scope, $modal) ->

    $scope.open = (size) ->
      modalInstance = $modal.open(
        templateUrl: "myModalIssueContent.html"
        controller: "IssueModalInstCtrl"
        size: size
      )
      modalInstance.result.then ( ->
      ), ->
]
.controller "IssueModalInstCtrl", ["$scope", "$http", "$modalInstance", "$timeout", "$log", ($scope, $http, $modalInstance, $timeout, $log) ->

    $scope.cancel = ->
      $modalInstance.dismiss "cancel"

    $scope.submit = (form) ->
      $scope.submitted = true
      return if form.$invalid

      $scope.inProgress = true

      $http
        method: "POST"
        data:
          "summary": form.summary.$viewValue
          "description": form.description.$viewValue
          "email": form.email.$viewValue
        url: "api/youtrack/createissue"
      .success (data, status) ->
        if status == 200
          $scope.successMsg = "Your issue has been successfully submitted. It will be listed here after it is reviewed by an engineer!";
          $scope.inProgress = false
        else
          $scope.errorMsg = "Oops, we received your request, but there was an error."
          $log.error data
        $timeout (->
          $scope.successMsg = null
          $scope.errorMsg = null
          $scope.submitted = false;
          $modalInstance.close "close"
        ), 4000
      .error (data, status, headers, config) ->
        $scope.progress = data
        $scope.errorMsg = "There was a network error. Please try again later."
        $log.error data
        $timeout (->
          $scope.errorMsg = null
          $scope.submitted = false;
          $modalInstance.close "close"
        ), 5000
]
.controller "ModalListingCtrl", ["$rootScope", "$scope", "$modal", ($rootScope, $scope, $modal) ->
    $scope.lst = $scope.$parent.allListings[$scope.$index]
    scope = $rootScope.$new()
    scope.lst = $scope.lst
    $scope.open = (size) ->
      modalInstance = $modal.open(
        scope: scope
        templateUrl: "modal-template.html"
        controller: "ListingModalInstCtrl"
        size: size
      )
      modalInstance.result.then ( ->
      ), ->
]
.controller "ListingModalInstCtrl", ["$scope", "$modalInstance", ($scope, $modalInstance) ->

    $scope.cancel = ->
      $modalInstance.dismiss "cancel"

    $scope.close = ->
      $modalInstance.dismiss "close"
]
.controller "ModalItemCarouselCtrl", ["$scope", ($scope) ->

    $scope.myInterval = 5000
    slides = $scope.slides = []
    $scope.addSlide = (i) ->
      slides.push
        image: "/pictures/" + $scope.$parent.lst.listing.pictures[i] + ".jpg"
        text: []

    if $scope.$parent.lst?
      i = 0
      l = $scope.$parent.lst.listing.pictures.length
      while i < l
        $scope.addSlide(i)
        i++
]
.controller "ItemCarouselCtrl", ["$scope", ($scope) ->

  $scope.myInterval = 5000
  slides = $scope.slides = []
  $scope.addSlide = (i) ->
    slides.push
      text: []
      active: false

  i = 0
  l = 20
  while i < l
    $scope.addSlide(i)
    i++
]
.controller "ListingCtrl", ["$scope", ($scope) ->

]
.controller "ProfileCtrl", ["$scope", "$timeout", "SocialService", ($scope, $timeout, SocialService) ->
    $scope.profileTemplate =
      url: "/partials/profile"
    $scope.status = ->
      FB.getLoginStatus (response) ->
        if response.status is "connected"
          uid = response.authResponse.userID
          accessToken = response.authResponse.accessToken
          expiresIn = response.authResponse.expiresIn
          FB.api "/me", (response) ->
            if response.email?
              SocialService.getSocial
                email: response.email
                info:
                  accessToken: accessToken
                  expiresIn: expiresIn
              , setupUI
        else if response.status is "not_authorized"
          console.log "not_authorized"
        else
          console.log "donno"
    setupUI = (logged) ->
      $scope.loggedIn = true if logged?
    if SocialService.social()? then setupUI(true)
    $timeout ->
      $scope.status()
    , 600
]
.controller "AlertCtrl", ["$scope", "$timeout", "ListingService", ($scope, $timeout, ListingService) ->

]