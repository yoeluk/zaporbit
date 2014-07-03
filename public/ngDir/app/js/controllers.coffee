"use strict"

# Controllers
angular.module("ZapOrbit.controllers", ["ngResource"]
).controller("AppCtrl", ["$scope", "$location", ($scope, $location) ->
    $scope.go = (path) ->
      $location.path path

]).controller("HomeCtr", ["$scope", "$http", "ngUrl", ($scope, $http, ngUrl) ->
  $scope.message = "Entice with higher confidence!"
  $scope.motivation = "Free App, lots of possibilities!"

]).controller("ShoppingCtrl", ["$scope", "LocationService", ($scope, LocationService) ->

  center =
    latitude: 45
    longitude: -73

  $scope.markerOption =
    visible: false
    title: "Your Location"

  $scope.map =
    center: center
    zoom: 8
    control: {}
    options:
      streetViewControl: false,
      panControl: false,
      maxZoom: 20,
      minZoom: 3

  $scope.coords = center

  showLocation = (coords) ->
    $scope.coords = coords
    $scope.markerOption.visible = true
    $scope.map.control.refresh
      latitude: coords.latitude
      longitude: coords.longitude
    $scope.map.control.getGMap().setZoom 12

  LocationService(showLocation)

]).controller("SupportCtrl", ["$scope", "trackUrl", "$http", "ngUrl", ($scope, youtrack, $http, ngUrl) ->

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
      url: youtrack + "getstats"
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
      url: youtrack + "allissues"
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

]).controller("HeaderController", ["$scope", "$location", ($scope, $location) ->

  $scope.isActive = (viewLocation) ->
    return viewLocation == $location.path()

]).controller("ModalIssueCtrl", ["$scope", "$modal", "$log", ($scope, $modal, $log) ->

  $scope.open = (size) ->
    modalInstance = $modal.open(
      templateUrl: "myModalIssueContent.html"
      controller: "ModalInstanceCtrl"
      size: size
    )
    modalInstance.result.then ( ->
    ), ->

]).controller("ModalInstanceCtrl", ["$scope", "$http", "$modalInstance", "$timeout", "trackUrl", ($scope, $http, $modalInstance, $timeout, youtrack) ->

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
        url: youtrack + "createissue"
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

]).controller("ListingItemController", ["$scope", ($scope) ->

])