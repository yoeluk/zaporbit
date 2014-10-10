"use strict"

# Controllers
angular.module "ZapOrbit.controllers", ["ngResource"]
.controller "AppCtrl", ["$scope", "$location", "StorageSupport", "$log", ($scope, $location, StorageSupport, $log) ->
  $scope.go = (path) ->
    $location.path path

  testSupportLocalStorage = ($scope) ->
    if !StorageSupport.hasStorage() then $log.warn "localStorage access failed"
    else $log.info "localStorage is supported"
  testSupportLocalStorage()
]
.controller "HeaderCtrl", ["$scope", "$location", "SocialService", "FacebookLogin", "$timeout", "$log", ($scope, $location, SocialService, FacebookLogin, $timeout, $log) ->

  $scope.isLoggedIn = false

  $scope.userIcon = ->
    glyphicon : true
    'glyphicon-user' : true

  $scope.userName = "Offline"

  $scope.$watch ($scope) ->
    SocialService.isLoggedIn()
  , (newValue) ->
    $scope.isLoggedIn = if newValue? then newValue else false
    if newValue? && FacebookLogin.getFbUser()?
      $scope.userName = FacebookLogin.getFbUser().first_name
    else $scope.userName = "Offline"

  $scope.loginStatus = (caching) ->
    FacebookLogin.getLoginStatus(caching, setupUI)

  setupUI = (auth) ->
    $timeout ->
      $scope.isLoggedIn = if auth? then auth else false

  $scope.loginStatus(true)

  $scope.isActive = (viewLocation) ->
    return viewLocation == $location.path()
]
.controller "UserHomeCtrl", ["$scope", "$timeout", "FacebookLogin", "$log", "$window", ($scope, $timeout, FacebookLogin, $log, $window) ->

  $scope.loadingMessage = "Loading..."

  $scope.title = "Public Summary"

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

  catchedUrl = $scope.profileTemplates[1].url

  setupUI = (auth) ->
    $timeout ->
      $scope.loadingProg = false
      if auth == true
        if FacebookLogin.getFbUser().id == $window.FB.getUserID()
          rand = Math.floor((Math.random() * 1000) + 1)
          $scope.profileTemplates[1].url = $scope.profileTemplates[1].url + "?r=" + rand
          $scope.profileTemplate = $scope.profileTemplates[1]
          $scope.userTemplate = $scope.userTemplates[1]
          catchedUrl = $scope.profileTemplates[1].url
        else $scope.profileTemplates[1].url = catchedUrl
      else
        $scope.userTemplate = $scope.userTemplates[0]
        $scope.profileTemplate = $scope.profileTemplates[0]

  $scope.loginStatus(true)

]
.controller "SecuredHomeCtrl", ["$scope", "$http", "FacebookLogin", "$log", "$window", '$routeParams',"$timeout", "$rootScope",
  ($scope, $http, FacebookLogin, $log, $window, $routeParams, $timeout, $rootScope) ->

    $scope.loadingProg = true

    randId = Math.floor((Math.random() * 1000) + 1)

    $scope.tabs = [
      {
        name: "Profile"
        icon: "glyphicon-user"
      }
      {
        name: "Messages"
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
        url: "/partials/profileprofile?id=" + randId
      }
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

    $scope.messagesExampleTemplate =
      url: "messages-example-template.html"

    $scope.transactionPills = [
      {
        name: "Requested"
      }
      {
        name: "Commited"
      }
      {
        name: "Completed"
      }
      {
        name: "Failed"
      }
    ]
    $scope.billingPills = [
      {
        name: "Unpaid Bills"
      }
      {
        name: "Paid Bills"
      }
    ]

    $scope.myFbId = FacebookLogin.getFbUser().id

    $scope.replying = false

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
          $log.debug data
          setReplies()
          $scope.loadingProg = false

    getRecords()

    $scope.sendReply = (index) ->
      message = $scope.replies[$scope.activePill[$scope.activeTab]].trim()
      if message? and message.length > 0
        convo = $scope.conversations[$scope.activePill[$scope.activeTab]]
        reply =
          convid: convo.conversation.id
          recipientid: if convo.user1.fbuserid != $scope.myFbId then convo.user1.id else convo.user2.id
          message: message
        $scope.replying = true
        $http
          method: "POST"
          data: reply
          url: "/api/replytoconvo"
          'Content-Type': "application/json"
        .success (data, status) ->
          if data? and data['withId']?
            data.message.id = data['withId']
            data.message.date = Date.now()
            convo.conversation.messages.push data.message
            $scope.$broadcast "replied", index
          $scope.replying = false
          $scope.replies[$scope.activePill[$scope.activeTab]] = ""
          $scope.tempReply = ""
          if status.code == 200
            console.log data
        .error (error) ->
          $scope.replying = false
          console.log error

    $scope.isThisMe = (index) ->
      convo = $scope.conversations[$scope.activePill[$scope.activeTab]]
      userIds = {}
      userIds[convo.user1.id] = convo.user1.fbuserid
      userIds[convo.user2.id] = convo.user2.fbuserid
      senderid = convo.conversation.messages[index].senderid
      userIds[senderid] == $scope.myFbId

    $scope.sellerOrBuyer = ->
      if $scope.conversations? && $scope.conversations[$scope.activePill[$scope.activeTab]]?
        if $scope.conversations[$scope.activePill[$scope.activeTab]].user1.fbuserid != $scope.myFbId then "Buyer" else "Seller"

    $scope.withWho = ->
      if $scope.conversations? && $scope.conversations[$scope.activePill[$scope.activeTab]]?
        if $scope.conversations[$scope.activePill[$scope.activeTab]].user1.fbuserid != $scope.myFbId
          $scope.conversations[$scope.activePill[$scope.activeTab]].user1.name + " " + $scope.conversations[$scope.activePill[$scope.activeTab]].user1.surname
        else $scope.conversations[$scope.activePill[$scope.activeTab]].user2.name + " " + $scope.conversations[$scope.activePill[$scope.activeTab]].user2.surname

    $scope.meOrUsername = (user) ->
      if user.fbuserid == $scope.myFbId then "me"
      else user.name

    $scope.activeTab = if $routeParams? && $routeParams.id? then parseInt($routeParams.id) else 0

    $scope.recordTemplate = $scope.recordTemplates[$scope.activeTab]

    $scope.activePill = {}

    $scope.replies = {}

    $scope.tempReply = if $scope.replies[$scope.activePill[$scope.activeTab]]? then $scope.replies[$scope.activePill[$scope.activeTab]] else ""

    _.each $scope.tabs, (tab, i) ->
      $scope.activePill[i] = 0

    setReplies = ->
      _.each $scope.conversations, (convo, i) ->
        $scope.replies[i] = ""

    $scope.msgPulledRight = (index) ->
      userIds = {}
      user1id = $scope.conversations[$scope.activePill[$scope.activeTab]].user1.id
      user2id = $scope.conversations[$scope.activePill[$scope.activeTab]].user2.id
      userIds[user1id] = $scope.conversations[$scope.activePill[$scope.activeTab]].user1.fbuserid
      userIds[user2id] = $scope.conversations[$scope.activePill[$scope.activeTab]].user2.fbuserid
      senderid = $scope.conversations[$scope.activePill[$scope.activeTab]].conversation.messages[index].senderid
      userIds[senderid] == $scope.conversations[$scope.activePill[$scope.activeTab]].user2.fbuserid

    $scope.isSample = ->
      if $scope.recordTemplate.url == "message-template.html" && $scope.conversations?
        $scope.conversations.length == 0
      else false

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

    $scope.isActivePill = (index) ->
      $scope.activePill[$scope.activeTab] == index

    $scope.scrollToWidth = (event) ->
      $timeout ->
        element = if event.srcElement? then event.srcElement else event.target
        title = angular.element(element).find( "h4" )
        counter = title.find( "span" )
        title.animate
          scrollLeft: title.width() + counter.width()
        , "slow"

    $scope.scrollToStart = (event) ->
      $timeout ->
        element = if event.srcElement? then event.srcElement else event.target
        title = angular.element(element).find( "h4" )
        title.scrollLeft(0)

    $scope.osPadding = ->
      if $window.navigator.appVersion.indexOf("Mac") != -1 then {"padding-right":"25px"}
      else {"padding-right":"8px"}

    $scope.msgWrapperStyle = ->
      if $window.navigator.appVersion.indexOf("Mac") != -1 then {'margin-right': '-30px', 'padding-right': '40px'}
      else {"padding-right":"13px"}

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
        'width': 0+"%"
        #'width': 100*((50+user.rating)/(5*(user.ratingCount+10)))+"%"

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

      $scope.formattedPrice = (index) ->
        if $scope.allListings? and $scope.allListings[index]?
          listing = $scope.allListings[index].listing
          if listing.currency_code == "TRY"
              listing.price + " &#xf195; " + listing.currency_code
#          else if listing.currency_code == "RUB"
#            listing.price + " &#xf158;"
          else listing.formatted_price + " " + listing.currency_code
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
      url: "/api/youtrack/getstats"
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
      url: "/api/youtrack/allissues"
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
      url: "/api/youtrack/createissue"
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
.controller "ModalItemCarouselCtrl", ["$scope", "$filter", ($scope, $filter) ->

  $scope.myInterval = 5000
  slides = $scope.slides = []
  $scope.addSlide = (i) ->
    slides.push
      image: "/pictures/" + $filter('appendExt')($scope.$parent.lst.listing.pictures[i])
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

  $scope.htmlDescription = ""

  $scope.$on "description", (e, description) ->
    $scope.htmlDescription = description

]
.controller "ProfileCtrl", ["$scope", "$timeout", "FacebookLogin", "$log", "SocialService", "$window", ($scope, $timeout, FacebookLogin, $log, SocialService, $window) ->

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

  catchedUrl = $scope.profileTemplates[1].url

  setupUI = (auth) ->
    $timeout ->
      $scope.showTplt = true
      if auth == true
        if FacebookLogin.getFbUser().id == $window.FB.getUserID()
          rand = Math.floor((Math.random() * 1000) + 1)
          $scope.profileTemplates[1].url = $scope.profileTemplates[1].url + "?r=" + rand
          $scope.profileTemplate = $scope.profileTemplates[1]
          catchedUrl = $scope.profileTemplates[1].url
        else $scope.profileTemplates[1].url = catchedUrl
      else $scope.profileTemplate = $scope.profileTemplates[0]

  $timeout ->
    $scope.loginStatus(true)
]
.controller "AlertCtrl", ["$scope", "$timeout", "ListingService", ($scope, $timeout, ListingService) ->

]
.controller "MessagesExampleCtrl", ["$scope", "$timeout", "$window", ($scope, $timeout, $window) ->

  $scope.showAlert = false

  $timeout ->
    $scope.showAlert = true
  , 200

  $scope.myFbId = 1234
  $scope.sampleConversations = $scope.conversations = [
    {
      title: 'First edition of the "On the origin of species" - Great conditions!'
      user1:
        name: "John"
        surname: "Smith"
        id: 1
        fbuserid: 1234
      user2:
        name: "Diana"
        surname: "Windsor"
        id: 2
        fbuserid: 2345
      messages: [
        {
          message: "These messages are generated in place of real messages that you might receive when selling or buying an item on ZapOrbit. These examples demonstrate the simplicity of the site."
          senderid: 1
          recipientid: 2
          date: Date.now()
        }
        {
          message: 'In this hypothetical case you are interested in Diana\'s item "On the origin of species". Your messages to Diana will appear here with a light green background.'
          senderid: 1
          recipientid: 2
          date: Date.now()
        }
        {
          message: 'Diana\'s replies will appear on this side. This order reflects the buyer and seller relationship.'
          senderid: 2
          recipientid: 1
          date: Date.now()
        }
      ]
    }
    {
      title: "Mountain bike in good conditions - large frame"
      user1:
        name: "Diana"
        surname: "Windsor"
        id: 1
        fbuserid: 2345
      user2:
        name: "John"
        surname: "Smith"
        id: 2
        fbuserid: 1234
      messages: [
        {
          message: 'In this other hypothetical case Diana is interested in your item and she contacted you asking you how old is the bike and if it comes with front lights, for example. Her message will appear here.'
          senderid: 1
          recipientid: 2
          date: Date.now()
        }
        {
          message: 'Diana is the buyer and you are the seller then your replies to Diana\'s questions appears on this side.'
          senderid: 2
          recipientid: 1
          date: Date.now()
        }
      ]
    }
  ]

  $scope.scrollToWidth = (event) ->
    $timeout ->
      element = if event.srcElement? then event.srcElement else event.target
      title = angular.element(element).find( "h4" )
      counter = title.find( "span" )
      title.animate
        scrollLeft: title.width() + counter.width()
      , "slow"

  $scope.scrollToStart = (event) ->
    $timeout ->
      element = if event.srcElement? then event.srcElement else event.target
      title = angular.element(element).find( "h4" )
      title.scrollLeft(0)

  $scope.deleteConvo = (event, index) ->
    element = `event.srcElement ? event.srcElement : event.target;`
    $scope.sampleConversations.splice(index,1)

  $scope.activeConvo = 0

  $scope.isThisMe = (index) ->
    convo = $scope.sampleConversations[$scope.activeConvo]
    userIds = {}
    userIds[convo.user1.id] = convo.user1.fbuserid
    userIds[convo.user2.id] = convo.user2.fbuserid
    senderid = convo.messages[index].senderid
    userIds[senderid] == $scope.myFbId

  $scope.sellerOrBuyer = ->
    if $scope.sampleConversations? && $scope.sampleConversations[$scope.activeConvo]?
      if $scope.sampleConversations[$scope.activeConvo].user1.fbuserid != $scope.myFbId then "Buyer" else "Seller"

  $scope.withWho = ->
    if $scope.sampleConversations? && $scope.sampleConversations[$scope.activeConvo]?
      if $scope.sampleConversations[$scope.activeConvo].user1.fbuserid != $scope.myFbId
        $scope.sampleConversations[$scope.activeConvo].user1.name + " " + $scope.sampleConversations[$scope.activeConvo].user1.surname
      else $scope.sampleConversations[$scope.activeConvo].user2.name + " " + $scope.sampleConversations[$scope.activeConvo].user2.surname

  $scope.meOrUsername = (user) ->
    if user.fbuserid == $scope.myFbId then "me"
    else user.name

  $scope.setActivePill = (index) ->
    $scope.activeConvo = index

  $scope.isActivePill = (index) ->
    $scope.activeConvo == index

  $scope.msgPulledRight = (index) ->
    userIds = {}
    user1id = $scope.sampleConversations[$scope.activeConvo].user1.id
    user2id = $scope.sampleConversations[$scope.activeConvo].user2.id
    userIds[user1id] = $scope.sampleConversations[$scope.activeConvo].user1.fbuserid
    userIds[user2id] = $scope.sampleConversations[$scope.activeConvo].user2.fbuserid
    senderid = $scope.sampleConversations[$scope.activeConvo].messages[index].senderid
    userIds[senderid] == $scope.sampleConversations[$scope.activeConvo].user2.fbuserid

  $scope.osPadding = ->
    if $window.navigator.appVersion.indexOf("Mac") != -1 then {"padding-right":"25px"}
    else {"padding-right":"8px"}

  $scope.msgWrapperStyle = ->
    if $window.navigator.appVersion.indexOf("Mac") != -1 then {'margin-right': '-30px', 'padding-right': '40px'}
    else {"padding-right":"13px"}
]
.controller "ProfileProfileCtrl", ["$scope", "$timeout", "$upload", "$log", ($scope, $timeout, $upload, $log) ->

  $scope.profileMenus = [
    {
      name: "Listings"
    }
    {
      name: "Feedbacks"
    }
    {
      name: "Following"
    }
    {
      name: "Merchant"
    }
  ]

  templates = [
    {
      name: "listingTemplate"
      url: "own-listings-template"
    }
  ]

  $scope.template = templates[0]

  activeMenu = $scope.profileMenus[0]

  $scope.isActive = (index) ->
    active: $scope.profileMenus[index].name == activeMenu.name

  $scope.setActive = (index) ->
    if templates[index]? then $scope.template = templates[index]
    else $scope.template = ""
    activeMenu = $scope.profileMenus[index]

  updateData = {}

  $scope.$on "aboutMe", (e, aboutMe) ->
    updateData.aboutMe = aboutMe

  profileData =
    profilePicture:
      src: ""
    backgroundPicture:
      src: ""

  pictureName = (size) ->
    name = "";
    chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@*_$";
    i = 0
    while i < size
      name += chars.charAt(Math.floor(Math.random() * chars.length));
      ++i
    name

  $scope.startEditing = ->
    $scope.$broadcast "activate"
    updateData.fakeProp = ""

  $scope.cancelEditing = ->
    setProfileData(profileData)
    updateData = {}

  doneEditing = ->
    turns = []
    turns.push
      prop: 'about'
      value: if updateData.aboutMe? and updateData.aboutMe != "" then updateData.aboutMe else ""
    hasPictures = false
    propsLength = Object.getOwnPropertyNames(updateData).length
    _.each Object.getOwnPropertyNames(updateData), (prop, i) ->
      if profileData[prop]? && updateData[prop] != "" then profileData[prop] = updateData[prop]
      if prop == "profilePicture" || prop == "backgroundPicture"
        hasPictures = true
        file = updateData[prop].file
        fileReader = new FileReader()
        fileReader.readAsArrayBuffer file
        parts = file.name.split "."
        fileName = pictureName(24) + "." + parts[1]
        turns.push
          value: fileName
          prop: prop
        fileReader.onload = (e) ->
          $upload.http
            url: '/api/saveOptionsPictures/'+fileName
            method: 'POST'
            data: e.target.result
            headers:
              'Content-Type': file.type
              'Content-Disposition':
                filename: fileName
          .progress (evt) ->
            console.log 'percent: ' + parseInt(100.0 * evt.loaded / evt.total)
          .success (data, status, headers, config) ->
            if propsLength == i+1
              updateOptions = {}
              _.each turns, (t) ->
                if t.value != ""
                  updateOptions[t.prop] = t.value
              updateUserOptions(updateOptions)

    setProfileData(profileData)
    if !hasPictures and updateData.aboutMe? and updateData.aboutMe != ""
      updateUserOptions
        about: updateData.aboutMe
    updateData = {}

  updateUserOptions = (data) ->
    $upload.http
      url: '/api/updateuseroptions'
      method: 'POST'
      data: data
    .success (data, status, headers) ->
      ''
    .error (error) ->
      console.log error

  $scope.isEditing = ->
    if Object.getOwnPropertyNames(updateData).length > 0 then true else false

  setProfileData = (data) ->
    $scope.profilePicture = data.profilePicture.src
    $scope.backgroundPicture = data.backgroundPicture.src

  setProfileData(profileData)

  $scope.onPictureSelect = ($files) ->
    file = $files[0]
    if file.type == "image/jpeg" || file.type == "image/png" || file.type == "image/jpg"
      fileReader = new FileReader();
      fileReader.readAsDataURL(file);
      fileReader.onload = (e) ->
        $timeout ->
          $scope.profilePicture = e.target.result
          updateData.profilePicture =
            src: e.target.result
            file: file
          doneEditing()

  $scope.onBackgroundSelect = ($files) ->
    file = $files[0]
    if file.type == "image/jpeg" || file.type == "image/png" || file.type == "image/jpg"
      fileReader = new FileReader();
      fileReader.readAsDataURL(file);
      fileReader.onload = (e) ->
        $timeout ->
          $scope.backgroundPicture = e.target.result
          updateData.backgroundPicture =
            src: e.target.result
            file: file
          doneEditing()

]
.controller "OwnListingsCtrl", ["$scope", "$timeout", "ListingsForUser", ($scope, $timeout, ListingsForUser) ->

  setOwnListings = (listings) ->
    $scope.ownListings = listings

  ListingsForUser.getListingsForUser
    remote: true
    callback: setOwnListings

  $scope.canList = (index) ->
    if $scope.ownListings?
      lst = $scope.ownListings[index]
      if lst.listingStatus.status == 'idle' || lst.listingStatus.status == 'none' then {}
      else {disable: true}

  $scope.canWithdraw = (index) ->
    if $scope.ownListings?
      lst = $scope.ownListings[index]
      if lst.listingStatus.status == 'forsale' then {}
      else {disable: true}

  $scope.canRelist = (index) ->
    return {disable: true}
#    if $scope.ownListings?
#      lst = $scope.ownListings[index]
#      if lst.listingStatus.status == 'sold' then {}
#      else {disable: true}

  $scope.canShare = (index) ->
    {disable: true}

  $scope.canUpdate = (index) ->
    if $scope.ownListings?
      lst = $scope.ownListings[index]
      if lst.listingStatus.status != 'committed' then {}
      else {disable: true}

  $scope.canDelete = (index) ->
    if $scope.ownListings?
      lst = $scope.ownListings[index]
      if lst.listingStatus.status != 'committed' then {}
      else {disable: true}

  $scope.updateStatus = (index, status) ->
    ListingsForUser.updateListingStatus index, status, setOwnListings

  $scope.share = (lst) ->
    FB.ui
      method: 'feed',
      name: lst.listing.title,
      link: 'https://zaporbit.com/#!/listing_item/'+lst.listing.id,
      picture: 'https://zaporbit.com/pictures/'+lst.listingPicture.name,
      description: lst.listing.description,

]
.controller "UserProfileCtrl", ["$scope", "UserListingService", "$routeParams", "$timeout", ($scope, UserListingService, $routeParams, $timeout) ->

  $scope.profileMenus = [
    {
      name: "Listings"
    }
    {
      name: "Feedbacks"
    }
  ]

  templates = [
    {
      name: "listingTemplate"
      url: "user-listings-template"
    }
  ]

  $scope.template = templates[0]

  activeMenu = $scope.profileMenus[0]

  $scope.isActive = (index) ->
    active: $scope.profileMenus[index].name == activeMenu.name

  $scope.setActive = (index) ->
    if templates[index]? then $scope.template = templates[index]
    else $scope.template = ""
    activeMenu = $scope.profileMenus[index]

  $scope.allListings = undefined

  gotListings = ( lst ) ->
    $scope.allListings = lst.listings
    $scope.$broadcast "UserListings", lst.listings

  UserListingService.getData($routeParams.id, gotListings)

]
.controller "UserListingsCtrl", ["$scope", ($scope) ->
  $scope.lst = {}
  $scope.$on "UserListings", (event, listings) ->
    $scope.lst.listings = listings
]
.controller "ModalUserItemCarouselCtrl", ["$scope", "$filter", ($scope, $filter) ->

  $scope.myInterval = 5000
  slides = $scope.slides = []
  $scope.addSlide = (i) ->
    slides.push
      image: "/pictures/" + $filter('appendExt')($scope.$parent.lst.pictures[i])
      text: []

  if $scope.$parent.lst?
    i = 0
    l = $scope.$parent.lst.pictures.length
    while i < l
      $scope.addSlide(i)
      i++
]
.controller "ModalNewListingCtrl", ["$scope", "$modal", ($scope, $modal) ->

  $scope.open = (size) ->
    modalInstance = $modal.open
      templateUrl: "new-listing-template.html"
      controller: "NewListingInstCtrl"
      size: size
      backdrop: 'static'
    modalInstance.result.then ( ->
    ), ->
]
.controller "NewListingInstCtrl", ["$rootScope", "$scope", "$filter", "$http", "$modalInstance", "$timeout", "$upload", "NewListingService"
  ($rootScope, $scope, $filter, $http, $modalInstance, $timeout, $upload, NewListingService) ->

    Array::move = (from, to) ->
      @splice to, 0, @splice(from, 1)[0]

    $scope.textAngularOpts = {}
    $scope.htmlDescription = ''
    $scope.errorMsg = "We found errors while processing your listing. Please ensure that all all fields are correctly filled and try again."
    $scope.showError = false
    $scope.inProgress = false
    $scope.uploadProgress = 0
    $scope.progMessage = ""
    $scope.pictureNames = []
    $scope.picturesProg = {}
    $scope.cancelTitle = "Cancel"
    $scope.posted = false
    $scope.pictures = []

    localeOpts =
      locale: 'en-US'
      currency_code: 'USD'

    $scope.sortableOptions =
      start: (e, ui) ->
        $(e.target).data("ui-sortable").floating = true

    $scope.cancel = ->
      $modalInstance.dismiss "cancel"

    $scope.onPictureSelect = ($files) ->
      file = $files[0]
      if file.type == "image/jpeg" || file.type == "image/png" || file.type == "image/jpg"
        fileReader = new FileReader()
        fileReader.readAsDataURL file
        fileReader.onload = (e) ->
          $timeout ->
            $scope.pictures.push
              src: e.target.result
              file: file

    $scope.deletePicture = (index) ->
      $timeout ->
        $scope.$apply ->
          $scope.pictures.splice index, 1

    $scope.$on "localeOpts", (e, opts) ->
      localeOpts = opts

    dataIsValid = (form) ->
      return false if (!form.title.$viewValue || $filter('trim')(form.title.$viewValue) == '' ||
        !form.price.$viewValue || !$filter('isNumber')(form.price.$viewValue) ||
        $filter('trim')($scope.htmlDescription) == '')
      return true

    $scope.submit = (form) ->
      $scope.submitted = true
      console.log form
      return if !dataIsValid form
      $scope.inProgress = true
      $scope.disableCancel = true
      if $scope.pictures.length
        doPictureUploading $scope.pictures, form
      return

    doPictureUploading = (pictures, form) ->
      length = pictures.length
      $scope.uploadProgress = 5
      _.each pictures, (p, i) ->
        $scope.picturesProg[i] = 0
      _.each pictures, (pic, i) ->
        file = pic.file
        fileReader = new FileReader()
        fileReader.readAsArrayBuffer file
        #bufView = new Uint16Array buf
        parts = file.name.split "."
        fileName = $filter('pictureName')(24) + "." + parts[1]
        fileReader.onload = (e) ->
          $upload.http
            url: '/api/uploadpictures/'+fileName
            method: 'POST'
            data: e.target.result
            headers:
              'Content-Type': file.type
              'Content-Disposition':
                filename: fileName

          .progress (evt) ->
            $scope.picturesProg[i] = parseInt (100.0 * evt.loaded / evt.total)
            sum = 0
            _.each pictures, (p, ii) ->
              sum += ($scope.picturesProg[ii] / length)
            $scope.uploadProgress = sum

          .success (data, status) ->
            $scope.pictureNames.push fileName
            if $scope.uploadProgress > 99
              console.log $scope.pictureNames
              $scope.progMessage = "Creating listing..."
              NewListingService.newListing
                title: $filter('trim')(form.title.$viewValue)
                description: $filter('trim')($scope.htmlDescription)
                price: parseFloat form.price.$viewValue
                locale: localeOpts.locale
                currency_code: localeOpts.currency_code
              , $scope.pictureNames, newListingCallback

    newListingCallback = (data) ->
      console.log data
      $scope.pictureNames = []
      $scope.posted = true
      $scope.inProgress = false
      $scope.disableCancel = false
      $scope.cancelTitle = "Dismiss"
      $scope.progMessage = ''

]