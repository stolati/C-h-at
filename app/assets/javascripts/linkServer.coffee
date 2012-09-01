#link_serv:connected
#link_serv:disconnected


define(['module', 'log', 'heart'], (module, log, heart) ->

  ###
  linkServer is the link to the server, receiving msg like any other element
  ###

  LINKSERVER_STATE =
    CONNECTING : "CONNECTING"
    CONNECTED : "CONNECTED"
    NOT_CONNECTED : "NOT_CONNECTED"
    ON_ERROR : "ON_ERROR"

  LINKSERVER_MSG =
    CONNECTED : "linkserv:connected"
    DISCONNECTED : "linkserv:disconnected"

  class LinkServer
    waiting_msg : [] #temporary msg waiting connection
    state : LINKSERVER_STATE.CONNECTING

    to_serv_use_msg : module.config().to_serv_use_msg
    to_serv_rename_msg : module.config().to_serv_rename_msg
    from_serv_rename : module.config().from_serv_rename


    constructor: () ->
      _.bindAll @
      @connect()

    connect : (uri = document.location.host) ->
      #TODO if a webservice exists before
      @baseURI = uri
      @wsURI = "ws://#{uri}/ws"

      @ws = new WebSocket(@wsURI)

      @ws.onopen = @onopen
      @ws.onclose = @onclose
      @ws.onmessage = @onmessage
      @ws.onerror = @onerror

    #connection of server part
    onopen : (evt) ->
      @state = LINKSERVER_STATE.CONNECTED
      heart.trigger(LINKSERVER_MSG.CONNECTED)

    onclose : (evt) ->
      @state = LINKSERVER_STATE.NOT_CONNECTED
      heart.trigger(LINKSERVER_MSG.DISCONNECTED)

    onmessage : (evt) ->
      msgJson = JSON.parse(evt.data)
      log.info "reception of :", evt.data
      [type, data] = [msgJson["kind"], msgJson["data"]]

      newName = @from_serv_rename[type]
      heart.trigger(newName, data)

    onerror : (evt) ->
      @state = LINKSERVER_STATE.ON_ERROR
      heart.trigger(LINKSERVER_MSG.DISCONNECTED)
      log.warn "linkServer closed on error : ", evt

    #connection to the client part
    onAllMsg : (event, args...) ->
      log.info "linkServer message : #{event}(#{args.join(',')})" #TODO remove when prod

      isToUse = event in @to_serv_use_msg
      nameRename = @to_serv_rename_msg[event]

      if not isToUse and not nameRename?
         log.info "don't take into account"
         return

      name = nameRename or event
      data = JSON.stringify({kind:name, data : args})
      log.info "sending data : '#{data}'"
      ws.send(data)


  sl = _.extend(new LinkServer(), Backbone.Events)
  sl.on("all", sl.onAllMsg)

  sl
)

