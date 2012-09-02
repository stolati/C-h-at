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
    heart : null
    convert : null


    constructor: (heart, convert) ->
      _.bindAll @
      @heart = heart
      @convert = convert

      @linkMsg()
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
      @heart.trigger(LINKSERVER_MSG.CONNECTED)

      #send the waiting messages
      for msg in @waiting_msg
        @onMsg.apply(@, msg)
      @waiting_msg = []

    onclose : (evt) ->
      @state = LINKSERVER_STATE.NOT_CONNECTED
      @heart.trigger(LINKSERVER_MSG.DISCONNECTED)

    onmessage : (evt) ->
      msgJson = JSON.parse(evt.data)
      log.info "reception of :", evt.data

      params = @convert.serv2cli(msgJson)
      @heart.trigger.apply(@heart, params)

    onerror : (evt) ->
      @state = LINKSERVER_STATE.ON_ERROR
      @heart.trigger(LINKSERVER_MSG.DISCONNECTED)
      log.warn "linkServer closed on error : ", evt

    #connection to the client part
    onMsg : (params...) ->
      if this.state != LINKSERVER_STATE.CONNECTED
        @waiting_msg.push(params)
        return

      msg = @convert.cli2serv(params)
      log.debug "sending data : ", msg
      data = JSON.stringify(msg)
      @ws.send(data)

    linkMsg : () ->
      for name in @convert.getListCliNames()
        @heart.on(name, _.bind(@onMsg, @, name))




  ###
  MessageConvertion, because the server have a hash of value, the client have parameters
  do the convertion between the parameters name - value of the hash
  ###
  class MessageConvertion
    clientName : null
    argumentNames : null
    serverName : null

    constructor : (cliPatt, servPatt) ->
      @serverName = servPatt
      @exploseCliPatt(cliPatt)

    exploseCliPatt : (cliPatt) ->
      name_param = /^([\w:]+)\(([\s\w,]*)\)$/
      param_sep = /\s*,\s*/

      matchRes = cliPatt.match(name_param)
      if not matchRes? then throw Error("MessageConvertion patt don't match : '#{cliPatt}'")
      [dummy, @clientName, paramStr] = matchRes
      @argumentNames = _(paramStr.split(param_sep)).filter( (e) -> not not e)

    servData2cliParam : (servEvent) ->
      args = (servEvent[name] for name in @argumentNames)
      args.unshift(@clientName)
      args

    cliParam2servData : (cliParam) ->
      [name, args...] = cliParam
      data = {'_t' : @serverName}
      for name in @argumentNames
        data[name] = args.shift()

      return data

    @getServDataName : (msg) -> msg["_t"]
    @getCliParamName : (msg) -> msg[0]

  class MessageConvertionList
    cli2servHash : {}
    serv2cliHash : {}

    constructor : (cli2servHash, serv2cliHash) ->
      @initCli2serv(cli2servHash)
      @initServ2cli(serv2cliHash)

    initCli2serv : (hash) ->
      for key, value of hash
        mc = new MessageConvertion(key, value)
        @cli2servHash[mc.clientName] = mc

    initServ2cli : (hash) ->
       for key, value of hash
         mc = new MessageConvertion(value, key)
         @serv2cliHash[mc.serverName] = mc

    cli2serv : (msg) ->
      name = MessageConvertion.getCliParamName(msg)
      mc = @cli2servHash[name]
      if not mc? then throw Error("MessageConvertion for the message cli->serv '#{msg}' is not found")
      mc.cliParam2servData(msg)

    serv2cli : (msg) ->
      name = MessageConvertion.getServDataName(msg)
      mc = @serv2cliHash[name]
      if not mc? then throw Error("MessageConvertion for the message serv->cli '#{msg}' is not found")
      mc.servData2cliParam(msg)

    getListCliNames: () -> (key for key of @cli2servHash)


  convert = new MessageConvertionList( module.config().to_serv, module.config().from_serv)
  new LinkServer(heart, convert)

  {
    LINKSERVER_STATE : LINKSERVER_STATE,
    LINKSERVER_MSG : LINKSERVER_MSG,
    LinkServer : LinkServer,
    MessageConvertion : MessageConvertion,
    MessageConvertionList : MessageConvertionList
  }
)

