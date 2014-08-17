"use strict"

# Services

angular.module("ZapOrbit.services", [])

.factory("LocationService", [ ->

  coords = undefined

  getLocation = (callback) ->
    displayPosition = (position) ->
      coords = position.coords
      callback(coords)

    displayError = (error) ->
      errors =
        1: 'Permission denied'
        2: 'Position unavailable'
        3: 'Request timeout'
      console.log "Error: " + errors[error.code]

    if !coords
      if  navigator.geolocation
        timeoutVal = 10 * 1000 * 1000
        navigator.geolocation.getCurrentPosition displayPosition, displayError,
          enableHighAccuracy: true
          timeout: timeoutVal
          maximumAge: 1
      else
        console.log "geolocation not supported by this browser"
    else callback(coords)
])

.value "version", "0.1"