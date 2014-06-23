"use strict"

# Controllers
angular.module("ZapOrbit.controllers", ["ngResource"]
).controller("AppCtrl", ["$scope", "$location", ($scope, $location) ->
    $scope.go = (path) ->
      $location.path path

]).controller("HomeCtr", ["$scope", "$http", "ngUrl", ($scope, $http, ngUrl) ->
  $scope.message = "ZapOrbit helps you build a reputation. Buying and selling locally create a close link between seller and buyer. Your reputation in ZapOrbit grows with your trade and the App puts a star rating based on your average feedback. The more stars the higher the confidence in your business!"
  $scope.motivation = "Free App, lots of possibilities!"

]).controller("AboutCtrl", [ ->

    displayPosition = (position) ->
      console.log "Latitude: " + position.coords.latitude + ", Longitude: " + position.coords.longitude
    displayError = (error) ->
      errors =
        1: 'Permission denied'
        2: 'Position unavailable'
        3: 'Request timeout'
      console.log "Error: " + errors[error.code]

    if navigator.geolocation
      timeoutVal = 10 * 1000 * 1000
      console.log "getting the location"
      navigator.geolocation.getCurrentPosition displayPosition, displayError,
        enableHighAccuracy: true
        timeout: timeoutVal
        maximumAge: 1
    else
      console.log "geolocation not supported"

    initialize = ->
      mapOptions =
        center: new google.maps.LatLng(-34.397, 150.644)
        zoom: 8
      map = new google.maps.Map(document.getElementById("map-canvas"), mapOptions)
      console.log "map initialized"
      return
    initialize()

    console.log "location supported"

]).controller("SupportCtrl", ["$scope", "trackUrl", "$http", "ngUrl", ($scope, youtrack, $http, ngUrl) ->

  $scope.allIssues = []
  $scope.oneAtATime = false
  $scope.isopen = false

  $scope.getIssues = getIssues = ->
    $http
      method: "GET"
      url: youtrack + "allissues"
      context: this
    .success (data, status) ->
      if status == 200
        $scope.allIssues = data["issue"]
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
    .error (data, status, headers, config) ->
  getIssues()

]).controller("HeaderController", ["$scope", "$location", ($scope, $location) ->

  $scope.isActive = (viewLocation) ->
    return viewLocation == $location.path()

]).controller("ModalIssueCtrl", ["$scope", "$modal", "$log", ($scope, $modal, $log) ->
  $scope.items = [
    "item1"
    "item2"
    "item3"
  ]
  $scope.open = (size) ->
    modalInstance = $modal.open(
      templateUrl: "myModalIssueContent.html"
      controller: "ModalInstanceCtrl"
      size: size
      resolve:
        items: ->
          $scope.items
    )
    modalInstance.result.then ((selectedItem) ->
      $scope.selected = selectedItem
      return
    ), ->
      $log.info "Modal dismissed at: " + new Date()
      return

    return

  return

]).controller("ModalInstanceCtrl", ["$scope", "$modalInstance", "items", ($scope, $modalInstance, items) ->
    $scope.items = items
    $scope.selected = item: $scope.items[0]

    $scope.ok = ->
      $modalInstance.close $scope.selected.item
      return

    $scope.cancel = ->
      $modalInstance.dismiss "cancel"
      return

    return
])