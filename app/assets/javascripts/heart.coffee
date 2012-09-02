
define(['module', 'log', 'external/backbone', 'external/underscore'], (module, log, backbone, _) ->

  ###
  heart module provide the global msg dispatch and rename
  ###

  #because Backbone.Events is not a class, the construction is a bit explosed
  class GlobalEvent
    names : {}

    constructor : () -> _.bindAll(this)

    #event that will be renamed when launched
    setName : (name, newName) ->
      @names[name] = newName

    #set multi event rename
    setNames : (hash) ->
      for key of hash
        @setName(key, hash[key])

    #callback for each msg
    onAllMsg : (event, args...) ->
      log.info "Global message : ", event, args #TODO remove when prod

      newName = @names[event]
      if newName?
        log.trace "Global message rename : #{event} => #{newName}"
        @trigger.apply(@, [newName].concat(args))

  #Backbone.Events ain't a class
  ge = _.extend(new GlobalEvent(), Backbone.Events)

  ge.on("all", ge.onAllMsg)

  #get config from main
  ge.setNames(module.config().msg_rename)

  ge
)


