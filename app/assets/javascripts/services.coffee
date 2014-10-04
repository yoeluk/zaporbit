"use strict"

# Services
angular.module "ZapOrbit.services", []
.factory "StorageSupport", ["localStorageService", (localStorageService) ->
    hasStorage = ->
      try
        localStorageService.set "test", "test"
        localStorageService.remove "test"
        true
      catch exception
        false
    hasStorage: hasStorage
]
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
.factory "SessionInjector", ["$injector", ($injector) ->

    request: (config) ->
      SocialService = $injector.get('SocialService')
      if SocialService.social()?
        config.headers["X-Auth-Token"] = SocialService.social().token
      config
]
.factory "SocialService", ["$injector", "localStorageService", "$timeout", "$log", ($injector, localStorageService, $timeout, $log) ->

    localStorageService.remove 'tokenData'
    localStorageService.remove 'loggedIn'

    inProgress = false

    logout = ->
      localStorageService.remove 'tokenData'
      localStorageService.remove 'loggedIn'

    login = (tokenData) ->
      localStorageService.set 'tokenData', tokenData
      localStorageService.set 'loggedIn', true

    isLoggedIn = ->
      localStorageService.get 'loggedIn'

    social = ->
      localStorageService.get 'tokenData'

    setupUIs = []

    requestSocialAuth = (body, setupUI) ->
      if !isLoggedIn()
        $http = $injector.get '$http'
        inProgress = true
        $http
          method: "POST"
          data: body
          url: '/auth/api/authenticate/facebook'
        .success (data, status) ->
          inProgress = false
          if data? && data.token?
            login(data)
            _.each setupUIs, (cb) ->
              cb(true)
            setupUIs = []
          else
            logout()
            _.each setupUIs, (cb) ->
              cb(false)
            setupUIs = []
        .error (error) ->
          $log.warn "There was an error getting the social user with error message: " + error
          inProgress = false

    getSocial = (body, setupUI) ->
      _.each setupUI, (stUI) ->
        if setupUIs.indexOf(stUI) == -1 then setupUIs.push stUI
      $timeout ->
        if inProgress then getSocial(body, setupUI)
        else requestSocialAuth(body, setupUI)
      , 50


    getSocial: getSocial
    social: social
    logout: logout
    isLoggedIn: isLoggedIn
]
.factory "FacebookLogin", ["$window", "SocialService", "$log", "$timeout", ($window, SocialService, $log, $timeout) ->

    callbacks = []

    fbUser = undefined

    setUICallback = (callback)->
      if callbacks.indexOf(callback) == -1 then callbacks.push callback


    setFacebookId = (user) ->
      fbUser = user

    getFbUser = ->
      fbUser

    getFbMe = (accessToken, expiresIn) ->
      $window.FB.api "/me", (user) ->
        if user.email?
          setFacebookId user
          SocialService.getSocial
            email: user.email
            info:
              accessToken: accessToken
              expiresIn: expiresIn
          , callbacks

    statusCallback = (response) ->
      if response.status is "connected"
        uid = response.authResponse.userID
        accessToken = response.authResponse.accessToken
        expiresIn = response.authResponse.expiresIn
        if getFbUser()?&& getFbUser().id != uid
          SocialService.logout()
          fbUser = undefined
          getFbMe(accessToken, expiresIn)
          return
        if SocialService.social()?
          _.each callbacks, (cb) ->
            cb(true)
        else
          getFbMe(accessToken, expiresIn)
      else if response.status is "not_authorized"
        $log.warn "not_authorized"
        SocialService.logout()
        fbUser = undefined
        _.each callbacks, (cb) ->
          cb(false)
        callbacks = []
      else
        $log.warn "donno"
        SocialService.logout()
        fbUser = undefined
        _.each callbacks, (cb) ->
          cb(false)
        callbacks = []

    getLoginStatus = (caching, callback) ->
      setUICallback(callback)
      $timeout ->
        if $window.FB? then $window.FB.getLoginStatus statusCallback, caching
        else getLoginStatus(caching, callback)
      , 250

    getLoginStatus: getLoginStatus
    getFbUser: getFbUser

]
.factory "ListingsForUser", ["$http", "$timeout", "$log", ($http, $timeout, $log) ->

  listings = undefined

  setListings = (lst) ->
    listings = lst

  listingsForUser = (callback) ->
    $http
      method: "GET"
      url: '/api/offersbyuser/0/1'
    .success (data, status) ->
      setListings(data.reverse())
      callback(data)

  getListingsForUser = (param) ->
    if listings? && !param.remote then callback(listings)
    else listingsForUser(param.callback)

  updateListingStatus = (index, status, callback) ->
    $http
      method: "POST"
      data:
        listingid: listings[index].listing.id
        status: status
      url: '/api/updatelistingstatus'
    .success (data) ->
      if data? and data.status == "OK"
        listings[index].listingStatus.status = status
        callback(listings)

  getListingsForUser: getListingsForUser
  updateListingStatus: updateListingStatus
]
.factory "UserListingService", ["$http", "$timeout", ($http, $timeout) ->

  ownListings = undefined

  setListings = (data) ->
    ownListings = data

  listings = ->
    ownListings

  getData = (userid, callback) ->
    $http
      method: "GET"
      url: "/api/listingsforuser/"+userid+"/0/1"
    .success (data, status) ->
      if data?
        results =
          listings: []
        _.each data, ( listing ) ->
          if listing.pictures.length > 0
            t = listing.updated_on.split /[- :]/
            d = new Date t[0], t[1]-1, t[2], t[3], t[4], t[5]
            listing.date = d
            results.listings.push listing
        setListings results
        callback ownListings

  getData: getData
]