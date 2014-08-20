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
.controller "ShoppingCtrl", ["$timeout", "$scope", "LocationService", "ReverseGeocode", ($timeout, $scope, LocationService, ReverseGeocode) ->

    $scope.submit = (form) ->
      $scope.submitted = true
      return if form.$invalid
      $scope.inProgress = true
      console.log "search submitted" + form
      $scope.inProgress = false

    zoom = 10
    timeInMs = 500

    $scope.showMap = navigator.geolocation
    $scope.showSearch = !navigator.geolocation

    $scope.markerOption =
      visible: false
      title: "Your Location"

    if LocationService.coords()
      $scope.markerOption.visible = true
      $scope.coords = LocationService.coords()
      if ReverseGeocode.address() then console.log ReverseGeocode.address()
      zoom = 13
    else
      $scope.coords =
        latitude: 37.77
        longitude: -122.22

    $scope.map =
      center: $scope.coords
      zoom: zoom
      control: {}
      panTo: false
      options:
        streetViewControl: false,
        panControl: false,
        maxZoom: 20,
        minZoom: 3

    displayError = (error) ->
      errors =
        1: 'Permission denied'
        2: 'Position unavailable'
        3: 'Request timeout'
      $scope.$apply (->
        $scope.showMap = false
        $scope.showSearch = true)

    addressCallback = (addr) ->
      console.log addr

    showLocation = (latlng) ->
      if $scope.map.control.getGMap?
        $scope.coords = latlng
        $scope.markerOption.visible = true
        $scope.map.control.getGMap().setZoom 13
        $scope.map.control.refresh
          latitude: latlng.latitude
          longitude: latlng.longitude
        ReverseGeocode.geocodeAddress(latlng.latitude, latlng.longitude, addressCallback)
      else
        $timeout (->
          showLocation(latlng)
        ), timeInMs

    if $scope.showMap && !LocationService.coords() then LocationService.location showLocation, displayError

]
.controller "SupportCtrl", ["$scope", "$http", ($scope, $http) ->

    $scope.allIssues = []
    $scope.oneAtATime = false
    $scope.isopen = true

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
          #console.log data
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
          i = 0
          l = $scope.allIssues.length
          while i < l
            issue = $scope.allIssues[i]
            issue.props = {}
            ii = 0
            ll = issue["field"].length
            while ii < ll
              prop =  issue["field"][ii]["name"]
              val = issue["field"][ii]["value"]
              issue.props[prop] = val
              ++ii
            ++i
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
        controller: "ModalInstanceCtrl"
        size: size
      )
      modalInstance.result.then ( ->
      ), ->

]
.controller "ModalInstanceCtrl", ["$scope", "$http", "$modalInstance", "$timeout", "$log", ($scope, $http, $modalInstance, $timeout, $log) ->

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
        ), 6000
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
.controller "ListingCtrl", ["$scope", ($scope) ->

]