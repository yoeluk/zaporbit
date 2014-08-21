"use strict"

# Services
angular.module "ZapOrbit.services", []
.factory "LocationService", [ ->
    coords = undefined
    setCoords = (co) ->
      coords =
        latitude: co.latitude
        longitude: co.longitude
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
              if comp.types[0] == "administrative_area_level_1" then addr.administrativeArea = comp.long_name
              if comp.types[0] == "route" then addr.street = comp.long_name
              if comp.types[0] == "street_number" then addr.number = comp.long_name
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
.factory "ListingService", ["$http", ($http) ->
    allListings = undefined
    setAllListings = (listings) ->
      allListings = listings
    getListings = (zoLoc, callback)->
      if !allListings
        $http
          method: "POST"
          data: zoLoc
          url: "/api/listingsbylocation/0/5"
          context: this
        .success (data, status) ->
          setAllListings(data)
          callback(data)
      else
        callback(allListings)
    listings: getListings
]