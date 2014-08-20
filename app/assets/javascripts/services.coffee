"use strict"

# Services
angular.module "ZapOrbit.services", []
.factory "LocationService", [ ->
    coords = undefined
    setCoords = (co) ->
      coords = co
    getCoords = ->
      coords
    getLocation = (callback, displayError) ->
      displayPosition = (position) ->
        setCoords(position.coords)
        callback(position.coords)
      locError = (error) ->
        displayError error
      if !coords
        if navigator.geolocation
          timeoutVal = 2 * 60 * 1000
          navigator.geolocation.getCurrentPosition displayPosition, locError,
            enableHighAccuracy: true
            timeout: timeoutVal
            maximumAge: 1
        else
          console.log "geolocation not supported by this browser"
      else callback(coords)
    location: getLocation
    coords: getCoords
]
.factory "ReverseGeocode", [ ->
    address = undefined
    setAddress = (addr) ->
      address = addr
    getAddress = ->
      address
    geocodeAddress = (lat, lng, callback) ->
      geocoder = new google.maps.Geocoder()
      latlng = new google.maps.LatLng(lat, lng)
      geocoder.geocode
        latLng: latlng
      , (results, status) ->
        if status is google.maps.GeocoderStatus.OK
          if results && results[0]
            addr = {}
            i = 0
            l = results[0].address_components.length
            while i < l
              comp = results[0].address_components[i]
              if comp.types[0] == "locality" then addr.locality = comp.long_name
              if comp.types[0] == "administrative_area_level_1" then addr.region = comp.long_name
              i++
            setAddress(addr)
            callback(addr)
          else
            console.log "Location not found"
        else
          console.log "Geocoder failed due to: " + status

    geocodeAddress: geocodeAddress
    address: getAddress
]