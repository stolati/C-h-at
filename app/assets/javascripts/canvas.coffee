
###
Map interface
define events : login:init, login:success, login:failed
###

#TODO can have a list of users while typing
#TODO have a stuff saying processing while waiting for the server
#TODO have a short window saying connected while connected (like staying 5 seconds)

define(['log', 'heart'], (log, heart) ->



  #window.log = console.log
  #
  ##TODO change the MapSurface so it's recreated for each map, not updated when map change
  #
  #Color = #enumerator of rgb
  #  white : [255, 255, 255]
  #  black : [0  , 0  , 0  ]
  #  green : [0  , 255, 0  ]
  #  red :   [255, 0  , 0  ]
  #  blue :  [0  , 0  , 255]
  #
  #
  ##old stuff to delete
  #myCanvasFct = ([x, y]) ->
  #
  ###################################
  ## Model
  ###################################
  #
  #class MapElement extends Backbone.Model
  #  defaults:
  #    posX : null
  #    posY : null
  #
  #  setPos : (x, y) -> @set({posX:x, posY:y})
  #  getPos : () -> [@get("posX"), @get("posY")]
  #
  #
  #class MapWall extends MapElement
  #
  #class MapPlayer extends MapElement
  #  idAttribute : "_id" #because play has it's own id on object
  #
  #class MapMainPlayer extends MapPlayer
  #
  #
  #class MapPlayers extends Backbone.Collection
  #  model : MapPlayer
  #
  #class MapWalls extends Backbone.Collection
  #  model : MapWall
  #
  #
  #class MapSurface extends Backbone.Model
  #  defaults:
  #    mainPlayer : null
  #    players : new MapPlayers
  #    mapSize : [1, 1]
  #    mapWalls : new MapWalls
  #  isInit : false
  #
  #  initialize: ->
  #    #options
  #    @canvasFct = ([x, y]) ->
  #
  #
  #  setCanvasFct: (fct) -> @canvasFct = fct
  #
  #  setMap: (data) ->
  #    h = data.length
  #    w = 0
  #    w = Math.max(w, e.length) for e in data
  #
  #    @get("mapWalls").reset([])
  #
  #    @set(mapSize:[w, h])
  #    for x in [0..w-1]
  #      for y in [0..h-1]
  #        switch data[y][x]["code"]
  #          when "F" then "Floor, do nothing"
  #          when "B"
  #            mw = new MapWall
  #            mw.setPos(x, y)
  #            @get("mapWalls").add(mw)
  #
  #    @isInit = true
  #
  #  moveAction: (where) ->
  #    return if not @isInit
  #
  #    [curX, curY] = @get("mainPlayer").getPos()
  #    [maxX, maxY] = @get("mapSize")
  #
  #    switch where
  #      when "left"  then curX -= 0.25
  #      when "right" then curX += 0.25
  #      when "up"    then curY -= 0.25
  #      when "down"  then curY += 0.25
  #      else return
  #
  #    curX = Math.min( Math.max(0, curX), maxX - 1)
  #    curY = Math.min( Math.max(0, curY), maxY - 1)
  #
  #    @get("mainPlayer").setPos(curX, curY)
  #    @canvasFct([curX, curY])
  #
  #  setPlayer: (id, [x, y]) ->
  #    [mainPlayer, players] = [@.get("mainPlayer"), @.get("players")]
  #    console.log mainPlayer
  #    if mainPlayer and id == mainPlayer.id
  #      el = mainPlayer
  #    else if players.get(id)
  #      el = players.get(id)
  #    else
  #      el = new MapPlayer({_id:id})
  #      players.add(el)
  #
  #    el.setPos(x, y)
  #
  #  rmPlayer: (id) ->
  #    players = @get("players")
  #    players.remove(players.get(id))
  #
  #  addMe: (id, [x, y]) ->
  #    console.log "adding me : " + [x, y]
  #    @set({mainPlayer:new MapPlayer({posX:x, posY:y, _id:id})})
  #    console.log @get("mainPlayer")
  #
  #
  #autoMove = (moves, looping) =>
  #  if moves.length == 0
  #    [move, time] = looping[0]
  #    looping = looping[1..] + looping[0..0]
  #  else
  #    [move, time] = moves[0]
  #    moves = moves[1..]
  #
  #  ms.moveAction(move)
  #  nextFct = () -> autoMove(moves, looping)
  #  setTimeout(nextFct, time)
  #
  #
  #
  ###################################
  ## View
  ###################################
  #
  #
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
  #
  #
  #
  #class CanvasDraw extends Backbone.View
  #
  #  isLinked : false
  #  hasContent = false
  #  mapX : null
  #  mapY : null
  #  step : 0
  #
  #  tagName: 'canvas'
  #  className : 'main_canvas'
  #
  #  initialize: ->
  #    #bind all the view's methods to this instance of view
  #    _.bindAll @
  #
  #    #@model.bind 'change', @render
  #    #@model.bind 'remove', @unbind
  #
  #    #@walls = new
  #    @step = @options.step
  #
  #    me = @
  #    @myCanvas_draw = (cv) ->
  #        cv.setup = () ->
  #           cv.size(Math.floor($(window).width() / 1.5), Math.floor($(window).height() / 1.3) )
  #           cv.background(255)
  #
  #        cv.draw = () -> me.render(cv)
  #
  #        cv.keyPressed = () -> me.move({37:"left", 39:"right", 38:"up", 40:"down"}[cv.keyCode])
  #
  #    @render()
  #
  #
  #
  #  #unrender: ->
  #  #  $(@el)remove()
  #
  #  render: (cv = null) ->
  #    if not @isLinked
  #      $("body").append(@el)
  #      @isLinked = true
  #
  #    return @ if cv == null
  #    return @ if not @model.isInit
  #
  #    [sizeX, sizeY] = @model.get("mapSize")
  #    cv.size(sizeX * @step, sizeY * @step)
  #    cv.background(0)
  #    cv.stroke(50)
  #
  #    #drawing 5 per 5 cases lines
  #    for x in [1..cv.width] by @step
  #        cv.line(x, 0, x, cv.height)
  #    for y in [1..cv.height] by @step
  #        cv.line(0, y, cv.width, y)
  #
  #    #drawing map floor
  #    [w, h] = @model.get("mapSize")
  #
  #    for [x, y] in @model.get("mapWalls").map((el) -> el.getPos())
  #      @drawSquare(cv, x, y, Color.white)
  #
  #    [mainPlayer, players] = [@model.get("mainPlayer"), @model.get("players")]
  #
  #    for [x, y] in players.map((el) -> el.getPos())
  #      @drawSquare(cv, x, y, Color.green)
  #
  #    [curP_x, curP_y] = mainPlayer.getPos()
  #    @drawSquare(cv, curP_x, curP_y, Color.red)
  #
  #
  #  drawSquare : (cv, x, y, color) ->
  #    [r, g, b] = color
  #    cv.fill(r, g, b)
  #    cv.rect(@step *x + 1, @step * y + 1, @step, @step)
  #
  #
  #  move: (direction) ->
  #    console.log "moving to " + direction
  #    @model.moveAction(direction)
  #
  #
  #
  #$(document).ready ->
  #
  #    Backbone.sync = (method, model, success, error) ->
  #      console.log "backbone.sync launched"
  #      success()
  #
  #    try
  #
  #        global_event = _.clone(Backbone.Events)
  #        global_event.on("all", (eventName) ->
  #          console.log "global event : " + eventName
  #
  #        )
  #
  #        console.log global_event
  #        console.log global_event.trigger("toto_event")
  #
  #        ms = new MapSurface({"toto": "titi"})
  #
  #        cd = new CanvasDraw({model: ms, step: 10})
  #
  #        document.cd = cd
  #        document.ms = ms
  #
  #        processing = new Processing(cd.el, cd.myCanvas_draw)
  #
  #        console.log "toto"
  #
  #        wsUri = document.location.host
  #        wsUri = "ws://#{wsUri}/ws" + document.location.search
  #
  #        ws = new WebSocket(wsUri)
  #        ws.onopen = (evt) ->
  #         console.log evt
  #         ws.send(JSON.stringify({kind:'Ask_Map', data: {}}))
  #
  #        ws.onclose = (evt) -> console.log evt
  #        ws.onmessage = (evt) ->
  #          console.log "reception of : ", evt.data
  #          msgJson = JSON.parse(evt.data)
  #          [type, data] = [msgJson["kind"], msgJson["data"]]
  #
  #          switch type
  #            when "CurrentMap"
  #              [my_body, others_body, cur_map] = [data["your_body"], data["others_body"], data["map"]["content"]]
  #              ms.setMap(cur_map)
  #              ms.addMe(my_body["id"]["id"], [my_body["pos"]["x"], my_body["pos"]["y"]])
  #              ms.setPlayer(el["id"]["id"], [el["pos"]["x"], el["pos"]["y"]]) for el in others_body
  #
  #              ms.setCanvasFct ([x, y]) ->
  #                  res =  JSON.stringify({kind:'Me_Move', data: { pos : {x:x, y:y} }})
  #                  console.log("sending : #{res}")
  #                  ws.send(res)
  #
  #            when "Player_Move" then ms.setPlayer(data["id"]["id"], [data["pos"]["x"], data["pos"]["y"]])
  #            when "Player_Join" then ms.setPlayer(data["id"]["id"], [data["pos"]["x"], data["pos"]["y"]])
  #            when "Player_Quit" then ms.rmPlayer(data["id"]["id"])
  #            when "YouQuit" then ms.init_empty()
  #            when "YouJump" then document.location.href = data["url"]
  #
  #            else
  #              console.log "no handler for that : "
  #              console.log evt.data
  #
  #        ws.onerror = (evt) -> console.log evt
  #
  #        i = 0
  #
  #        $.processing = processing
  #
  #        #autoMove([], [["right", 1000], ["up", 1000]])
  #
  #    catch error
  #        console.log error
  #
  #
	
)