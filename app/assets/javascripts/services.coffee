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
            maximumAge: 10
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
    geocodeAddress = (lat, lng, callback, query) ->
      geocoder = new google.maps.Geocoder()
      geocodeInput = undefined
      if query?
        geocodeInput = query
      else
        geocodeInput =
          latLng: new google.maps.LatLng(lat, lng)
      geocoder.geocode geocodeInput, (results, status) ->
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
            if !addr.street then addr.street = "none"
            setAddress(addr)
            callback addr, {
              latitude: results[0].geometry.location.lat()
              longitude: results[0].geometry.location.lng()
            }
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
          if data?
            i = 0
            l = data.length
            while i < l
              lst = data[i]
              t = lst.listing.updated_on.split(/[- :]/)
              d = new Date(t[0], t[1]-1, t[2], t[3], t[4], t[5])
              lst.listing.date = d
              i++
            setAllListings(data)
            callback(data)
      else
        callback(allListings)
    listings: getListings
]