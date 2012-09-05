
###
Map interface
define events : login:init, login:success, login:failed
###

#TODO can have a list of users while typing
#TODO have a stuff saying processing while waiting for the server
#TODO have a short window saying connected while connected (like staying 5 seconds)

define(['module', 'log', 'heart', 'external/kinetic'], (module, log, heart, kinetic) ->

  pos2Position = (pos) ->
    {_t:'Position', x : pos[0], y : pos[1]}


  Color = #enumerator of rgb
    white : [255, 255, 255]
    black : [0  , 0  , 0  ]
    green : [0  , 255, 0  ]
    red :   [255, 0  , 0  ]
    blue :  [0  , 0  , 255]


  MAP_STATUS =
    NO_MAP : "NO_MAP"
    ON_MAP : "ON_MAP"

  MAP_MSG =
    MAP_CONTENT : "map:content"
    MAP_QUIT : "map:quit"

    ME_MOVING : "map:me_moving"

    PLAYER_MOVE : "map:player_move"
    PLAYER_JOIN : "map:player_join"
    PLAYER_STATUS : "map:player_status"
    PLAYER_QUIT : "map:player_quit"




  class MapElement extends Backbone.Model
    defaults:
      posX : null
      posY : null

    setPos : (x, y) -> @set({posX:x, posY:y})
    getPos : () -> [@get("posX"), @get("posY")]


  class MapWall extends MapElement

  class MapPlayer extends MapElement
    idAttribute : "_id" #because play has it's own id on object

  class MapMainPlayer extends MapPlayer

  class MapPlayers extends Backbone.Collection
    model : MapPlayer

  class MapWalls extends Backbone.Collection
    model : MapWall



  class PlayMapModel extends Backbone.Model
    defaults:
      state : MAP_STATUS.NOT_MAP
      mainPlayer : null
      players : new MapPlayers
      mapSize : [1, 1]
      mapWalls : new MapWalls


    initialize: () ->
      _.bindAll @
      heart.on(MAP_MSG.MAP_CONTENT, @setMapContent)
      heart.on(MAP_MSG.MAP_QUIT, @mapQuit)

      #same function for join and status
      heart.on(MAP_MSG.PLAYER_STATUS, @playerStatus)
      heart.on(MAP_MSG.PLAYER_JOIN, @playerStatus)

      heart.on(MAP_MSG.PLAYER_QUIT, @playerQuit)


    ###############
    # response to msg
    ###############
    setMapContent : (my_body, others_body, cur_map) ->
      log.debug "have new map content"

      @setMap(cur_map)
      @addMe(my_body["id"]["id"], [my_body["pos"]["x"], my_body["pos"]["y"]])
      @setPlayer(el["id"]["id"], [el["pos"]["x"], el["pos"]["y"]]) for el in others_body

      log.debug "setting status to ", MAP_STATUS.ON_MAP
      @set('state', MAP_STATUS.ON_MAP)

      #              ms.setCanvasFct ([x, y]) ->
      #                  res =  JSON.stringify({kind:'Me_Move', data: { pos : {x:x, y:y} }})
      #                  console.log("sending : #{res}")
      #                  ws.send(res)


    mapQuit : () ->
      log.debug "map quit"

    playerStatus : (id, pos) ->
      log.debug "playerStatus"
      @setPlayer(id['id'], [pos['x'], pos['y']])

    playerQuit : (id) ->
      @rmPlayer(id['id'])


    ###############
    # change the content
    ###############
    setMap : (data) ->
      data = data['content']

      h = data.length
      w = 0
      w = Math.max(w, e.length) for e in data

      @get("mapWalls").reset([])
      @set('mapSize', [w, h])
      for x in [0..w-1]
        for y in [0..h-1]
          switch data[y][x]["code"]
            when "F" then "Floor, do nothing"
            when "B"
              mw = new MapWall
              mw.setPos(x, y)
              @get("mapWalls").add(mw)

    addMe: (id, [x, y]) ->
      @set({mainPlayer:new MapPlayer({posX:x, posY:y, _id:id})})

    setPlayer: (id, [x, y]) ->
      log.debug "setPlayer(#{id}, #{x}, #{y})"
      [mainPlayer, players] = [@.get("mainPlayer"), @.get("players")]
      if id == mainPlayer?.id
        log.debug "is from main player"
        el = mainPlayer
      else if players.get(id)
        log.debug "is from another player"
        el = players.get(id)
      else if players.get(id)
      else
        log.debug "is from a player not known"
        el = new MapPlayer({_id:id})
        players.add(el)

      el.setPos(x, y)
      @trigger("change:mainPlayer")
      @trigger("change:players")

    rmPlayer: (id) ->
      players = @get("players")
      players.remove(players.get(id))
      @trigger("change:players")

    moveAction: (where) ->
      log.debug "moving to ", where

      [curX, curY] = @get("mainPlayer").getPos()
      [maxX, maxY] = @get("mapSize")

      switch where
        when "left"  then curX -= 0.25
        when "right" then curX += 0.25
        when "up"    then curY -= 0.25
        when "down"  then curY += 0.25
        else return

      curX = Math.min( Math.max(0, curX), maxX - 1)
      curY = Math.min( Math.max(0, curY), maxY - 1)

      @get("mainPlayer").setPos(curX, curY)
      @trigger("change:mainPlayer")
      heart.trigger(MAP_MSG.ME_MOVING, pos2Position([curX, curY]))

  #class CanvasSquare extends Backbone.View
  #  color : Color.blue
  #  posX : null
  #  posY : null
  #
  #  render : (cv = null, conf = null) ->
  #    return @ if cv == null
  #
  #    drawSquare(cv, @posX, @posY, @color, conf["step"])
  #
  #  drawSquare : (cv, x, y, color, step) ->
  #    [r, g, b] = color
  #    cv.fill(r, g, b)
  #    cv.rect(step *x + 1, step * y + 1, step, step)
  #
  #class CanvasWall extends CanvasSquare
  #  color : Color.white
  #
  #
  #class CanvasPlayer extends Backbone.View
  #  color : Color.red
  #
  #
  #class CanvasMainPlayer extends Backbone.View
  #  color : Color.green






  class PlayMapView extends Backbone.View
    className : 'div'

    mapX : null
    mapY : null
    step : module.config().view_step

    stage : null
    floor_layer : null
    brick_layer : null
    player_layer : null

    render_ok : false

    #events :
    #  'keypress body' : "move"
    #  'keyup body' : "move"
    #  'keyevent body' : "move"

    initialize: ->
      _.bindAll @
      @model.bind('change:state', @setModelStatus)
      @model.bind('change:mainPlayer change:players change:mapSize change:mapWalls', @render)

      @stage   = new Kinetic.Stage({container : @el})
      @floor_layer = new Kinetic.Layer()
      @player_layer = new Kinetic.Layer()
      @brick_layer = new Kinetic.Layer()

      #@stage.add(@floor_layer)
      #@stage.add(@player_layer)
      @stage.add(@brick_layer)

      window.addEventListener('keydown', @move, true)

    setModelStatus : (ob, status, option) ->
      log.debug 'having a new status'

      switch status
        when MAP_STATUS.NO_MAP
          log.debug "no map"
          #@$el.hide()
          @render_ok = false

        when MAP_STATUS.ON_MAP
          #load map infos
          [sizeX, sizeY] = @model.get("mapSize")
          [@mapX, @mapY] = [sizeX * @step, sizeY * @step]

          #prepare the view
          $('body').append(@$el) if @$el.parents().size() == 0
          @stage.setSize(@mapX, @mapY)
          #set mapX mapY
          @$el.show()
          @render_ok = true

          #initialize the view

          @render()

      log.debug "new status of PlayMapModel : ", status

    render : () ->
      log.debug "rendering"
      if not @render_ok then return

      @brick_layer.clear()

      @brick_layer.add(new Kinetic.Rect({
        x : 0,
        y : 0,
        width : @mapX,
        height : @mapY,
        fill : "black",
        strokeWidth: 0
      }))

      [sizeX, sizeY] = @model.get("mapSize")

      #drawing 5 per 5 cases lines
      for x in [1..@mapX] by @step
          @brick_layer.add(new Kinetic.Line({
            points : [x, 0, x, @mapY],
            stroke : "white",
            strokeWidth: 1
          }))
      for y in [1..@mapY] by @step
          @brick_layer.add(new Kinetic.Line({
            points : [0, y, @mapX, y],
            stroke : "white",
            strokeWidth: 1
          }))

      #drawing map floor
      [w, h] = @model.get("mapSize")

      for [x, y] in @model.get("mapWalls").map((el) -> el.getPos())
        @drawSquare(@brick_layer, x, y, "white")

      [mainPlayer, players] = [@model.get("mainPlayer"), @model.get("players")]

      for [x, y] in players.map((el) -> el.getPos())
        @drawSquare(@brick_layer, x, y, "green")

      [curP_x, curP_y] = mainPlayer.getPos()
      @drawSquare(@brick_layer, curP_x, curP_y, "red")

      @stage.draw()

    drawSquare: (element, x, y, color) ->
      element.add(new Kinetic.Rect({
        x : @step * x + 1,
        y : @step * y + 1,
        width : @step,
        height : @step,
        fill : color,
        strokeWidth : 0
      }))

    move: (key_event) ->
      @model.moveAction( {37:"left", 39:"right", 38:"up", 40:"down"}[key_event.keyCode] )



  #        ws.onmessage = (evt) ->
  #          console.log "reception of : ", evt.data
  #          msgJson = JSON.parse(evt.data)
  #          [type, data] = [msgJson["kind"], msgJson["data"]]
  #
  #          switch type
  #            when "Player_Join" then ms.setPlayer(data["id"]["id"], [data["pos"]["x"], data["pos"]["y"]])
  #            when "Player_Quit" then ms.rmPlayer(data["id"]["id"])
  #            when "YouQuit" then ms.init_empty()
  #            when "YouJump" then document.location.href = data["url"]



  return {
    PlayMapModel : PlayMapModel
    PlayMapView : PlayMapView
    Color : Color
  }

)



