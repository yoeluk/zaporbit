"use strict"

# Controllers
angular.module("ZapOrbit.controllers", ["ngResource"])
.controller("AppCtrl", ["$scope", "$location", ($scope, $location) ->
    $scope.go = (path) ->
      $location.path path

]).controller("HomeCtr", ["$scope", "apiUrl", "$http", "ngUrl", ($scope, url, $http, ngUrl) ->

]).controller("AboutCtrl", [ ->

]).controller("SupportCtrl", [ ->

]).controller("HeaderController", ["$scope", "$location", ($scope, $location) ->
    $scope.isActive = (viewLocation) ->
      return viewLocation == $location.path()
])