
###
Map interface for the users part
define events : login:init, login:success, login:failed
###

#TODO can have a list of users while typing
#TODO have a stuff saying processing while waiting for the server
#TODO have a short window saying connected while connected (like staying 5 seconds)

define(['module', 'log', 'heart', 'external/kinetic', 'play_map'], (module, log, heart, kinetic, play_map) ->

  MAP_STATUS =
    NO_MAP : "NO_MAP"
    ON_MAP : "ON_MAP"

  MAP_MSG =
    MAP_CONTENT : "map:init"
    PLAYER_QUIT : "map:player_quit"
    MAP_QUIT : "map:quit"


    ME_MOVING : "map:me_moving"

    PLAYER_STATUS : "map:player_status"

  class MapPlayer extends play_map.MapElement
    default:
      color: 'red'
    idAttribute : "_id" #because play has it's own id on object


  class MapPlayerModel extends Backbone.Model
    defaults:
      state : MAP_STATUS.NOT_MAP
      mainPlayer : null
    changeValidated : null

    initialize: () ->
      _.bindAll @
      heart.on(MAP_MSG.MAP_CONTENT, @setMapContent)
      heart.on(MAP_MSG.PLAYER_QUIT, @playerQuit)
      heart.on(MAP_MSG.PLAYER_STATUS, @playerStatus)


    ###############
    # response to msg
    ###############
    setMapContent : (my_body, others_body, cur_map) ->
      id = my_body["id"]["id"]
      [x, y] = [my_body["pos"]["x"], my_body["pos"]["y"]]
      @set('mainPlayer', new MapPlayer({posX : x, posY: y, _id:id}))
      @changeValidated = true
      @set('state', MAP_STATUS.ON_MAP)

    playerQuit : (id) ->
      if @get('mainPlayer')?.id == id['id']
        log.debug "changing states to no map"
        @set('state', MAP_STATUS.NO_MAP)
        heart.trigger(MAP_MSG.MAP_QUIT)

    playerStatus : (id, pos) ->
      [id, x, y] = [id['id'], pos['x'], pos['y']]

      mp = @get("mainPlayer")

      if id != mp?.id then return

      log.debug "playerStatus = true"

      @changeValidated = true
      mp.setPos(x, y)
      @trigger("change:mainPlayer", @, mp)

    moveAction: (where) ->
      log.debug "moving to ", where
      log.debug "with changeValidated = ", @changeValidated
      if ! @changeValidated then return

      [curX, curY] = @get("mainPlayer").getPos()

      switch where
        when "left"  then curX -= 0.25
        when "right" then curX += 0.25
        when "up"    then curY -= 0.25
        when "down"  then curY += 0.25
        else return

      @get("mainPlayer").setPos(curX, curY)
      @changeValidated = false
      @trigger("change:mainPlayer", @, @get("mainPlayer"))
      heart.trigger(MAP_MSG.ME_MOVING, play_map.pos2Position([curX, curY]))



  class MapPlayerView extends Backbone.View
    className : 'div'

    mapX : null
    mapY : null
    step : module.config().view_step

    mapMain : null
    layer : new Kinetic.Layer()
    el : new Kinetic.Rect({
      x : 0,
      y : 0,
      width : module.config().view_step,
      height : module.config().view_step,
      fill : "red",
      strokeWidth : 0
    })

    initialize: ->
      _.bindAll @
      @mapMain = @options['mapMain']

      @model.bind('change:state', @statusChanged)
      @model.bind('change:mainPlayer', @render)

      @mapMain.addLayer(@layer)

      @layer.add(@el)

    statusChanged : (model, status, option) ->
      log.debug "state map_player changed", status
      switch status
        when MAP_STATUS.ON_MAP then window.addEventListener('keydown', @move, true)
        when MAP_STATUS.NO_MAP then window.removeEventListener('keydown', @move, true)

    render: () ->
      [posX, posY] = @model.get('mainPlayer').getPos()

      @el.setX(posX * @step)
      @el.setY(posY * @step)

      @layer.draw()

    move: (key_event) ->
      @model.moveAction( {37:"left", 39:"right", 38:"up", 40:"down"}[key_event.keyCode] )


  return {
    MapPlayerModel : MapPlayerModel
    MapPlayerView : MapPlayerView
  }

)

