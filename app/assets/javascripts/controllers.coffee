"use strict"

# Controllers
angular.module "ZapOrbit.controllers", ["ngResource"]
.controller "AppCtrl", ["$scope", "$location", "StorageSupport", "$log", ($scope, $location, StorageSupport, $log) ->
  $scope.go = (path) ->
    $location.path path

  $scope.$watch ($scope) ->
    StorageSupport.hasStorage()
  , (newValue) ->
    if !newValue then $log.warn "localStorage access failed"
]
.controller "HeaderCtrl", ["$scope", "$location", "SocialService", "FacebookLogin", "$timeout", ($scope, $location, SocialService, FacebookLogin, $timeout) ->

  $scope.isLoggedIn = false

  $scope.$watch ($scope) ->
    SocialService.isLoggedIn()
  , (newValue) ->
    $scope.isLoggedIn = if newValue? then newValue else false

  $scope.loginStatus = (caching) ->
    FacebookLogin.getLoginStatus(caching, setupUI)

  setupUI = (auth) ->
    $timeout ->
      $scope.isLoggedIn = if auth? then auth else false

  $scope.loginStatus(true)

  $scope.isActive = (viewLocation) ->
    return viewLocation == $location.path()
]
.controller "UserHomeCtrl", ["$scope", "$timeout", "FacebookLogin", ($scope, $timeout, FacebookLogin) ->

  $scope.loadingMessage = "Loading..."

  $scope.title = "Profile Summary"

  $scope.loadingProg = true

  $scope.userTemplates = [
    {
      url: "login-template.html"
    }
    {
      url: "userhome-template.html"
    }
  ]

  $scope.profileTemplates = [
    {
      url: "/loggedoutTemplate"
    }
    {
      url: "/partials/profile"
    }
  ]

  $scope.userTemplate = $scope.profileTemplate = undefined

  $scope.loginStatus = (caching) ->
    FacebookLogin.getLoginStatus(caching, setupUI)

  setupUI = (auth) ->
    $timeout ->
      $scope.loadingProg = false
      if auth == true
        $scope.userTemplate = $scope.userTemplates[1]
        $scope.profileTemplate = $scope.profileTemplates[1]
      else
        $scope.userTemplate = $scope.userTemplates[0]
        $scope.profileTemplate = $scope.profileTemplates[0]

  $scope.loginStatus(true)

]
.controller "SecuredHomeCtrl", ["$scope", "$http", "FacebookLogin", "$timeout", ($scope, $http, FacebookLogin, $timeout) ->

  $scope.tabs = [
    {
      name: "Message"
      icon: "glyphicon-envelope"
    }
    {
      name: "Purchases"
      icon: "glyphicon-credit-card"
    }
    {
      name: "Sales"
      icon: "glyphicon-transfer"
    }
    {
      name: "Billing"
      icon: "glyphicon-briefcase"
    }
  ]

  $scope.recordTemplates = [
    {
      url: "message-template.html"
    }
    {
      url: "purchase-template.html"
    }
    {
      url: "sale-template.html"
    }
    {
      url: "billing-template.html"
    }
  ]

  $scope.myFbId = FacebookLogin.getFbId()

  getRecords = ->
    $http
      method: "GET"
      url: "/api/getrecords/0"
    .success (data, status) ->
      if data?
        $scope.records = data
        $scope.conversations = $scope.records.messages_records
        _.each $scope.conversations, (convo) ->
          _.each convo.conversation.messages, (msg) ->
            t = msg.created_on.split /[- :]/
            d = new Date t[0], t[1]-1, t[2], t[3], t[4], t[5]
            msg.date = d
        console.log data
        setReplies()

  getRecords()

  $scope.meOrUsername = (user) ->
    if user.fbuserid == $scope.myFbId then "me"
    else user.name

  $scope.activeTab = 0

  $scope.activePill = {}

  $scope.replies = {}

  $scope.tempReply = if $scope.replies[$scope.activePill[$scope.activeTab]] is not undefined then $scope.replies[$scope.activePill[$scope.activeTab]] else ""

  _.each $scope.tabs, (tab, i) ->
    $scope.activePill[i] = 0

  setReplies = ->
    _.each $scope.conversations, (convo, i) ->
      $scope.replies[i] = ""
    console.log $scope.replies

  sendReply = ->
    console.log $scope.reply

  $scope.recordTemplate = $scope.recordTemplates[0]

  $scope.msgPulledRight = (index) ->
    userIds = {}
    user1id = $scope.conversations[$scope.activePill[$scope.activeTab]].user1.id
    user2id = $scope.conversations[$scope.activePill[$scope.activeTab]].user2.id
    userIds[user1id] = $scope.conversations[$scope.activePill[$scope.activeTab]].user1.fbuserid
    userIds[user2id] = $scope.conversations[$scope.activePill[$scope.activeTab]].user2.fbuserid
    senderid = $scope.conversations[$scope.activePill[$scope.activeTab]].conversation.messages[index].senderid
    userIds[senderid] == $scope.conversations[$scope.activePill[$scope.activeTab]].user2.fbuserid

  $scope.tabIcon = (index) ->
    classes = {}
    classes.glyphicon = true
    classes[$scope.tabs[index].icon] = true
    classes

  $scope.setActiveTab = (index) ->
    $scope.recordTemplate = $scope.recordTemplates[index]
    $scope.activeTab = index

  $scope.isActiveTab = (index) ->
    index == $scope.activeTab

  $scope.setActivePill = (index) ->
    $scope.activePill[$scope.activeTab] = index
    $scope.tempReply = $scope.replies[index]
    console.log "temp reply: " + $scope.tempReply


  $scope.isActivePill = (index) ->
    $scope.activePill[$scope.activeTab] == index

#  $timeout ->
#    if $scope.replies[$scope.activePill[$scope.activeTab]] is not undefined
#      $scope.replies[$scope.activePill[$scope.activeTab]] = $scope.tempReply
#  , 500
]
.controller "HomeCtr", ["$scope", ($scope) ->
    $scope.message = "Entice with higher confidence!"
    $scope.motivation = "Free App, lots of possibilities!"
]
.controller "ShoppingCtrl", ["$timeout", "$scope", "LocationService", "ReverseGeocode", "ListingService", "pageSize", "localStorageService", "$rootScope",
  ($timeout, $scope, LocationService, ReverseGeocode, ListingService, pageSize, localStorageService, $rootScope) ->

      $scope.markers = undefined

      $scope.getRatingWidth = (user) ->
        'width': 100*((50+user.rating)/(5*(user.ratingCount+10)))+"%"

      timeInMs = 500

      openListing = (index) ->
        $rootScope.$emit "openListing", index

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

      browserLoc = localStorageService.get('loc')

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

      $scope.showSearch = !navigator.geolocation

      listingCallback = (listings) ->
        $scope.allListings = listings
        $scope.paging = ListingService.paging()
        $scope.filterStr = ListingService.filter()
        if !$scope.filterStr && listings.length == 0 && $scope.alerts.length == 0
          $scope.addAlert "There are not listings in this location."
        doListingsMarkers()

      doListingsMarkers = ->
        $scope.markers = []
        _.each $scope.allListings, (lst, i) ->
          $scope.markers.push
            id: lst.listing.id
            coords:
              latitude: lst.location.latitude
              longitude: lst.location.longitude
            control: {}
            index: i
            options:
              visible: true
              title: lst.listing.title
              draggable: false
        _.each $scope.markers, (m) ->
          m.onClicked = ->
            openListing(m.index)

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
          title: ""
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
        $scope.$apply ->
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
        $scope.allIssues = data["issues"]
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

  $scope.cancelTitle = "Cancel"

  $scope.cancel = ->
    $modalInstance.dismiss "cancel"

  $scope.submit = (form) ->
    $scope.submitted = true
    return if form.$invalid
    $scope.inProgress = true
    $scope.disableCancel = true
    $http
      method: "POST"
      data:
        "summary": form.summary.$viewValue
        "description": form.description.$viewValue
        "email": form.email.$viewValue
      url: "api/youtrack/createissue"
    .success (data, status) ->
      if status == 200
        $scope.posted = true
        $scope.inProgress = false
        $scope.disableCancel = false
        $scope.cancelTitle = "Dismiss"
        $scope.successMsg = "Your issue has been successfully submitted. It will be listed here after it is reviewed by an engineer!";
      else
        $scope.errorMsg = "Oops, we received your request, but there was an error."
        $log.error data
        $scope.disableCancel = false
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
.controller "ModalListingCtrl", ["$rootScope", "$scope", "$modal", "$timeout", ($rootScope, $scope, $modal, $timeout) ->
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

  timeInMs = 250

  $rootScope.$on "openListing", (e, index) ->
    if !$rootScope.recentOpen && $scope.$index == index
      $rootScope.recentOpen = true
      $scope.open('lg')
      $timeout ->
        $rootScope.recentOpen = false
      , timeInMs
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
  l = 15
  while i < l
    $scope.addSlide(i)
    i++
]
.controller "ListingCtrl", ["$scope", ($scope) ->

]
.controller "ProfileCtrl", ["$scope", "$timeout", "FacebookLogin", "$log", "SocialService", ($scope, $timeout, FacebookLogin, $log, SocialService) ->

  $scope.title = "Profile Summary"
  $scope.showTplt = false
  $scope.profileTemplates = [
    {
      url: "/loggedoutTemplate"
    }
    {
      url: "/partials/profile"
    }
  ]

  $scope.profileTemplate = $scope.profileTemplates[0]

  $scope.loginStatus = (caching) ->
    FacebookLogin.getLoginStatus(caching, setupUI)

  setupUI = (auth) ->
    $timeout ->
      $scope.showTplt = true
      if auth == true then $scope.profileTemplate = $scope.profileTemplates[1]
      else $scope.profileTemplate = $scope.profileTemplates[0]

  $timeout ->
    $scope.loginStatus(true)
]
.controller "AlertCtrl", ["$scope", "$timeout", "ListingService", ($scope, $timeout, ListingService) ->

]