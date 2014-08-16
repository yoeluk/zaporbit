// Generated by CoffeeScript 1.7.1
(function() {
  "use strict";
  angular.module("ZapOrbit.controllers", ["ngResource"]).controller("AppCtrl", [
    "$scope", "$location", function($scope, $location) {
      return $scope.go = function(path) {
        return $location.path(path);
      };
    }
  ]).controller("HomeCtr", [
    "$scope", "$http", "ngUrl", function($scope, $http, ngUrl) {
      $scope.message = "Entice with higher confidence!";
      return $scope.motivation = "Free App, lots of possibilities!";
    }
  ]).controller("ShoppingCtrl", [
    "$scope", "LocationService", function($scope, LocationService) {
      var center, showLocation;
      $scope.submit = function(form) {
        $scope.submitted = true;
        if (form.$invalid) {
          return;
        }
        $scope.inProgress = true;
        console.log("search submitted" + form);
        return $scope.inProgress = false;
      };
      center = {
        latitude: 45,
        longitude: -73
      };
      $scope.markerOption = {
        visible: false,
        title: "Your Location"
      };
      $scope.map = {
        center: center,
        zoom: 8,
        control: {},
        options: {
          streetViewControl: false,
          panControl: false,
          maxZoom: 20,
          minZoom: 3
        }
      };
      $scope.coords = center;
      showLocation = function(coords) {
        $scope.coords = coords;
        $scope.markerOption.visible = true;
        $scope.map.control.refresh({
          latitude: coords.latitude,
          longitude: coords.longitude
        });
        return $scope.map.control.getGMap().setZoom(12);
      };
      return LocationService(showLocation);
    }
  ]).controller("SupportCtrl", [
    "$scope", "trackUrl", "$http", "ngUrl", function($scope, youtrack, $http, ngUrl) {
      var getIssues, getStats;
      $scope.allIssues = [];
      $scope.oneAtATime = false;
      $scope.isopen = true;
      $scope.bugs = 0;
      $scope.tasks = 0;
      $scope.features = 0;
      $scope.ioss = 0;
      $scope.webs = 0;
      getStats = function() {
        return $http({
          method: "GET",
          url: youtrack + "getstats",
          context: this
        }).success(function(data, status) {
          var count;
          if (status === 200) {
            count = data["count"];
            $scope.bugs = count[0];
            $scope.tasks = count[1];
            $scope.features = count[2];
            $scope.ioss = count[3];
            $scope.webs = count[4];
            return $scope.pipeline = true;
          }
        });
      };
      $scope.getIssues = getIssues = function() {
        return $http({
          method: "GET",
          url: youtrack + "allissues",
          context: this
        }).success(function(data, status) {
          var i, ii, issue, l, ll, prop, val;
          if (status === 200) {
            $scope.allIssues = data["issues"]["issue"];
            i = 0;
            l = $scope.allIssues.length;
            while (i < l) {
              issue = $scope.allIssues[i];
              issue.props = {};
              ii = 0;
              ll = issue["field"].length;
              while (ii < ll) {
                prop = issue["field"][ii]["name"];
                val = issue["field"][ii]["value"];
                issue.props[prop] = val;
                ++ii;
              }
              ++i;
            }
            return getStats();
          }
        }).error(function(data, status, headers, config) {});
      };
      return getIssues();
    }
  ]).controller("HeaderController", [
    "$scope", "$location", function($scope, $location) {
      return $scope.isActive = function(viewLocation) {
        return viewLocation === $location.path();
      };
    }
  ]).controller("ModalIssueCtrl", [
    "$scope", "$modal", "$log", function($scope, $modal, $log) {
      return $scope.open = function(size) {
        var modalInstance;
        modalInstance = $modal.open({
          templateUrl: "myModalIssueContent.html",
          controller: "ModalInstanceCtrl",
          size: size
        });
        return modalInstance.result.then((function() {}), function() {});
      };
    }
  ]).controller("ModalInstanceCtrl", [
    "$scope", "$http", "$modalInstance", "$timeout", "trackUrl", function($scope, $http, $modalInstance, $timeout, youtrack) {
      $scope.cancel = function() {
        return $modalInstance.dismiss("cancel");
      };
      return $scope.submit = function(form) {
        $scope.submitted = true;
        if (form.$invalid) {
          return;
        }
        $scope.inProgress = true;
        return $http({
          method: "POST",
          data: {
            "summary": form.summary.$viewValue,
            "description": form.description.$viewValue
          },
          url: youtrack + "createissue"
        }).success(function(data, status) {
          if (status === 200) {
            $scope.successMsg = "Your issue has been successfully submitted. It will be listed here after it is reviewed by an engineer!";
            $scope.inProgress = false;
          } else {
            $scope.errorMsg = "Oops, we received your request, but there was an error.";
            $log.error(data);
          }
          return $timeout((function() {
            $scope.successMsg = null;
            $scope.errorMsg = null;
            $scope.submitted = false;
            return $modalInstance.close("close");
          }), 6000);
        }).error(function(data, status, headers, config) {
          $scope.progress = data;
          $scope.errorMsg = "There was a network error. Please try again later.";
          $log.error(data);
          return $timeout((function() {
            $scope.errorMsg = null;
            $scope.submitted = false;
            return $modalInstance.close("close");
          }), 5000);
        });
      };
    }
  ]).controller("ListingCtrl", ["$scope", function($scope) {}]);

}).call(this);

//# sourceMappingURL=controllers.map
