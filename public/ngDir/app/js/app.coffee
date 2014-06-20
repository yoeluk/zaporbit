'use strict'

# Declare app level module which depends on filters, and services
angular.module("myApp", [
  "ngRoute",
  "myApp.filters",
  "myApp.services",
  "myApp.directives",
  "myApp.controllers",
  "infinite-scroll",
  "ui.bootstrap"
])
.constant("apiUrl", "http://youtrack.zaporbit.com/")
.constant("ngUrl", "http://omgexams.com/assets/ngApp/app/")
.config ["$routeProvider", "ngUrl", ($routeProvider, ngUrl) ->
  $routeProvider
  .when "/",
    templateUrl: ngUrl + "partials/home.html"
    controller: "HomeCtr"
  .when "/questions",
    templateUrl: ngUrl + "partials/questions.html"
    controller: "QuestsCtr"
  .when "/answers",
    templateUrl: ngUrl + "partials/answers.html"
    controller: "AnswsCtr"
  .otherwise redirectTo: "/"
]