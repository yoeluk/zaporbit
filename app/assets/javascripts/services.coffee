"use strict"

# Services
angular.module "ZapOrbit.services", []
.factory "LocationService", ["$timeout", ($timeout) ->

    that = {}

    setCoords = (co) ->
      that.coords =
        latitude: co.latitude
        longitude: co.longitude

    getCoords = ->
      that.coords

    getLocation = (callback, displayError, scope) ->
      displayPosition = (position) ->
        setCoords(position.coords)
        callback(position.coords)
      locError = (error) ->
        if that.progPromise? then $timeout.cancel(that.progPromise)
        displayError error
      if !that.coords
        if navigator.geolocation
          timeoutVal = 2 * 60 * 1000
          navigator.geolocation.getCurrentPosition displayPosition, locError,
            enableHighAccuracy: true
            timeout: timeoutVal
            maximumAge: 10
            if scope
              that.progPromise = $timeout ->
                scope.locProg = true
              , 200
        else
          console.log "geolocation not supported by this browser"
      else callback(that.coords)

    location: getLocation
    coords: getCoords
    setCoords: setCoords
]
.factory "ReverseGeocode", [ ->

    address = undefined
    coords = undefined

    setCoords = (c) ->
      coords = c
    getCoords = ->
      coords

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
          latLng: new google.maps.LatLng lat, lng
      geocoder.geocode geocodeInput, (results, status) ->
        if status is google.maps.GeocoderStatus.OK
          if results && results[0]
            addr = {}
            _.each results[0].address_components, (comp) ->
              addr.locality = comp.long_name if comp.types[0] == "locality"
              addr.administrativeArea = comp.long_name if comp.types[0] == "administrative_area_level_1"
              addr.street = comp.long_name if comp.types[0] == "route"
              addr.number = comp.long_name if comp.types[0] == "street_number"
            addr.street = "none" if !addr.street
            setAddress addr
            setCoords
              latitude: results[0].geometry.location.lat()
              longitude: results[0].geometry.location.lng()
            callback addr, coords
          else
            console.log "Location not found"
        else
          console.log "Geocoder failed due to: " + status

    geocodeAddress: geocodeAddress
    address: getAddress
    coords: getCoords
]
.factory "ListingService", ["$http", "pageSize", ($http, pageSize) ->

    allListings = undefined
    paging = undefined
    filterStr = undefined

    setAllListings = (d) ->
      allListings = d.listings
      paging = d.paging

    getPaging = ->
      paging

    getFilterStr = ->
      filterStr

    getData = (url, body, callback) ->
      $http
        method: "POST"
        data: body
        url: url
        context: this
      .success (data, status) ->
        if data?
          results =
            listings: []
            paging: data.paging
          _.each data.listings, (lst) ->
            if lst.listing.pictures.length > 0
              t = lst.listing.updated_on.split /[- :]/
              d = new Date t[0], t[1]-1, t[2], t[3], t[4], t[5]
              lst.listing.date = d
              results.listings.push lst
          setAllListings results
          callback allListings

    getListings = (body, callback, remote, filter, page) ->
      if !page then page = 0
      if (!filter || filter == "") && (!allListings || remote)
        getData "/api/listingsbylocation/"+page+"/10", body, callback
        filterStr = undefined
      else if filter
        getData "/api/filterlocation/" + page + "?filter=" + filter, body, callback
        filterStr = filter
      else
        callback allListings

    listings: getListings
    paging: getPaging
    filter: getFilterStr
]
.factory "hackedFB", [ ->

]
.factory "sessionInjector", ['$q', "$log", "SocialService", ($q, $log, SocialService) ->

    sessionInjector = request: (config) ->
      #deferred = $q.defer()
      if SocialService.social()?
        config.headers["X-Auth-Token"] = SocialService.social().token
      config

    sessionInjector
]
.factory "SocialService", ["$q", "$location", "$injector", ($q, $location, $injector) ->

    that = {}

    logout = ->
      that.social = undefined

    social = ->
      that.social

    getSocial = (body, setupUI) ->
      if !that.social
        $http = $injector.get '$http'
        $http
          method: "POST"
          data: body
          url: 'auth/api/authenticate/facebook'
          context: this
        .success (data, status) ->
          setupUI(data?)
          if data? && data.token?
            that.social = data
          else
            logout()

    getSocial: getSocial
    social: social
    logout: logout
]