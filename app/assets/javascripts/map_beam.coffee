
###
###

define(['module', 'log', 'heart', 'external/kinetic', 'play_map'], (module, log, heart, kinetic, play_map) ->

  getAngle_b = (AC, AB) -> return Math.tan(AC/AB)

  BEAM_STATUS =
    ACTIVATED : "ACTIVATED"
    DESACTIVATED : "DESACTIVATED"

  BEAM_MSG =
    INIT : "map:init"
    QUIT : "map:quit"

    PLAYER_STATUS : "map:player_status"

    LAUNCH_BEAM : "beam:launch"
    BEAM_LAUNCHED : "beam:launched"
    UPDATE_HP : "player:hp"


  class MapPlayer extends play_map.MapElement
    idAttribute : "_id" #because play has it's own id on object


  class MapBeamModel extends Backbone.Model
    defaults:
      state : BEAM_STATUS.DESACTIVATED
      mainPlayer : null
      mainPlayerId : null
      hp : null

    changeValidated : null

    initialize: () ->
      _.bindAll @
      heart.on(BEAM_MSG.INIT, @setMapContent)
      heart.on(BEAM_MSG.QUIT, @playerQuit)

      heart.on(BEAM_MSG.PLAYER_STATUS, @playerStatus)
      heart.on(BEAM_MSG.BEAM_LAUNCHED, @beamLaunched)
      heart.on(BEAM_MSG.UPDATE_HP, @updateHP)

    ###############
    # response to msg
    ###############
    setMapContent : (my_body, others_body, cur_map) ->
      id = my_body["id"]["id"]
      [x, y] = [my_body["pos"]["x"], my_body["pos"]["y"]]
      @set('mainPlayer', new MapPlayer({posX : x, posY: y, _id:id}))

      @set('id', id)
      @set('state', BEAM_STATUS.ACTIVATED)

    playerQuit : (id) ->
      if @get('mainPlayer')?.id == id['id']
        @set('state', BEAM_STATUS.DESACTIVATED)

    playerStatus : (id, pos) ->
      [id, x, y] = [id['id'], pos['x'], pos['y']]

      mp = @get("mainPlayer")

      if id != mp?.id then return

      mp.setPos(x, y)
      @trigger("change:mainPlayer", @, mp)

    beamLaunched : (pos_from, angle) ->
      @trigger("beamlaunched", pos_from, angle)

    launchBeam : (angle) ->
      heart.trigger(BEAM_MSG.LAUNCH_BEAM, angle)



  class MapBeamView extends Backbone.View
    className : 'div'

    step : module.config().view_step
    mousePos : [0, 0]
    playerPos : [0, 0]
    mapPos : [0, 0]

    mapMain : null
    layer : new Kinetic.Layer()
    el : new Kinetic.Line({
      points : [-1, -1],
      stroke : "blue",
      strokeWidth : 2,
    })

    initialize: ->
      _.bindAll @
      @mapMain = @options['mapMain']

      @model.bind('change:state', @statusChanged)
      @model.bind('change:mainPlayer', @changeMainPlayer)

      @mapMain.addLayer(@layer)

      @layer.add(@el)

    statusChanged : (model, status, option) ->
      log.debug "state map_player changed", status
      switch status
        when BEAM_STATUS.ACTIVATED
          window.addEventListener('mousemove', @move, true)
          window.addEventListener('click', @click, true)
        when BEAM_STATUS.DESACTIVATED
          window.removeEventListener('mousemove', @move, true)
          window.removeEventListener('click', @click, true)

    changeMainPlayer : (model, mp) ->
      [pX, pY] = mp.getPos()
      pX = (pX + 0.5) * @step
      pY = (pY + 0.5) * @step
      @playerPos = [pX, pY]

    render: () ->

      [playerPosX, playerPosY] = @playerPos
      [mousePosX, mousePosY] = @mousePos

      @el.setPoints([playerPosX, playerPosY, mousePosX, mousePosY])
      @layer.draw()

    move: (event) ->
      [mapPosX, mapPosY] = @mapPos
      @mousePos = [ event.pageX - mapPosX, event.pageY - mapPosY]
      @render()

    click : (event) ->

      [playerPosX, playerPosY] = @playerPos
      [mousePosX, mousePosY] = @mousePos

      a = getAngle_b( mousePosX - playerPosX, playerPosY - mousePosY )
      @model.launchBeam(a)

  return {
    MapBeamModel : MapBeamModel
    MapBeamView : MapBeamView
  }

)
