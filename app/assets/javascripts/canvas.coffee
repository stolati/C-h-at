window.log = console.log

Color = #enumerator of rgb
  white : [255, 255, 255]
  black : [0  , 0  , 0  ]
  green : [0  , 255, 0  ]
  red :   [255, 0  , 0  ]
  blue :  [0  , 0  , 255]


class SquarePlace extends Backbone.Model
    constructor: (@pos_x, @pos_y) ->

    move: (x, y) ->
        @pos_x += x
        @pos_y += y

#old stuff to delete
myCanvasFct = ([x, y]) -> 

listElems = {}
myId = ""

isInitalized = false

mapContent = null
mapSize = [1, 1]

class MapSurface extends Backbone.Model

  initialize: ->
    this.init_empty()
    @canvasFct = (id, [x, y]) ->
  
  setCanvasFct: (fct) -> @canvasFct = fct
  
  init_empty: () ->
    console.log "init_empty"
    @listElems = {}
    @id = ""
    @isInit = false
    @mapContent = null
    @mapSize = [1, 1]
  
  setMap: (data) ->
    @mapContent = ((cell["code"] for cell in line) for line in data)
    h = @mapContent.length
    w = 0
    w = Math.max(w, e.length) for e in @mapContent
    @mapSize = [w, h] #TODO
    @isInit = true

  moveAction: (where) ->
    return if not @isInit
    
    [curX, curY] = @listElems[@id]
    [maxX, maxY] = @mapSize
  
    switch where
      when "left"  then curX -= 0.25
      when "right" then curX += 0.25
      when "up"    then curY -= 0.25
      when "down"  then curY += 0.25
      else return
    
    curX = Math.min( Math.max(0, curX), maxX - 1)
    curY = Math.min( Math.max(0, curY), maxY - 1)
        
    @listElems[@id] = [curX, curY] 
    log "myCanvasFct( [#{curX}, #{curY}] )"
    @canvasFct(@id, [curX, curY])

  getAt: ([x, y]) ->
    return "None" if not @isInit
    
    switch @mapContent[y][x]
      when "F" then "Floor"
      when "B" then "Block"

  getAllPlayers: () ->
    [@listElems[@id] , (pos for el_id, pos of @listElems when el_id != @id)]

  setPlayer: (id, [x, y]) -> @listElems[id] = [x, y]
  rmPlayer: (id) -> delete @listElems[id]
  addMe: (id, [x, y]) -> [@id, @listElems[id]] = [id, [x, y]]

  


class Toto extends Backbone.View

  constructor: () ->
    @toto = "titi"

document.Toto = Toto

autoMove = (moves, looping) =>
  if moves.length == 0
    [move, time] = looping[0]
    looping = looping[1..] + looping[0..0]
  else
    [move, time] = moves[0]
    moves = moves[1..]
  
  ms.moveAction(move)
  nextFct = () -> autoMove(moves, looping)
  setTimeout(nextFct, time)



class CanvasSquare extends Backbone.View
  color : Color.blue
  posX : null
  posY : null

  render : (cv = null, conf = null) ->
    return @ if cv == null

    drawSquare(cv, @posX, @posY, @color, conf["step"])

  drawSquare : (cv, x, y, color, step) ->
    [r, g, b] = color
    cv.fill(r, g, b)
    cv.rect(step *x + 1, step * y + 1, step, step)



class CanvasWall extends CanvasSquare
  color : Color.white


class CanvasPlayer extends Backbone.View
  color : Color.red


class CanvasMainPlayer extends Backbone.View
  color : Color.green



class CanvasDraw extends Backbone.View

  isLinked : false
  hasContent = false
  mapX : null
  mapY : null
  step : 0

  tagName: 'canvas'
  className : 'main_canvas'

  initialize: ->
    #bind all the view's methods to this instance of view
    _.bindAll @

    #@walls = new
    @step = @options.step

    me = @
    @myCanvas_draw = (cv) ->
        cv.setup = () ->
           cv.size(Math.floor($(window).width() / 1.5), Math.floor($(window).height() / 1.3) )
           cv.background(255)

        cv.draw = () -> me.render(cv)

        cv.keyPressed = () -> me.move({37:"left", 39:"right", 38:"up", 40:"down"}[cv.keyCode])

    @render()


  #unrender: ->
  #  $(@el)remove()

  render: (cv = null) ->
    if not @isLinked
      $("body").append(@el)
      @isLinked = true

    return @ if cv == null
    return @ if not @model.isInit

    [sizeX, sizeY] = @model.mapSize
    cv.size(sizeX * @step, sizeY * @step)
    cv.background(Color.black)
    cv.stroke(50)

    #drawing 5 per 5 cases lines
    for x in [1..cv.width] by @step
        cv.line(x, 0, x, cv.height)
    for y in [1..cv.height] by @step
        cv.line(0, y, cv.width, y)

    #drawing map floor
    [w, h] = @model.mapSize

    for x in [0..w-1]
      for y in [0..h-1]
        curColor = switch @model.getAt([x, y])
          when "None" then Color.blue
          when "Floor" then Color.black
          when "Block" then Color.white
          else Color.blue

        @drawSquare(cv, x, y, curColor)

    [[curP_x, curP_y], otherPlayers] = @model.getAllPlayers()

    for [x, y] in otherPlayers
      @drawSquare(cv, x, y, Color.green)

    @drawSquare(cv, curP_x, curP_y, Color.red)


  drawSquare : (cv, x, y, color) ->
    [r, g, b] = color
    cv.fill(r, g, b)
    cv.rect(@step *x + 1, @step * y + 1, @step, @step)


  move: (direction) ->
    console.log "moving to " + direction
    @model.moveAction(direction)



$(document).ready ->

    Backbone.sync = (method, model, success, error) ->
      console.log "backbone.sync launched"
      success()


    try

        ms = new MapSurface({"toto": "titi"})

        cd = new CanvasDraw({model: ms, step: 10})

        document.cd = cd
        document.ms = ms

        processing = new Processing(cd.el, cd.myCanvas_draw)
        
        console.log "toto"
        
        wsUri = document.location.host
        wsUri = "ws://#{wsUri}/ws" + document.location.search
        
        ws = new WebSocket(wsUri)
        ws.onopen = (evt) ->
         console.log evt

         ws.send(JSON.stringify({kind:'Ask_Map', data: {}}))
        ws.onclose = (evt) -> console.log evt
        ws.onmessage = (evt) ->
          console.log "reception of : ", evt.data
          msgJson = JSON.parse(evt.data)
          [type, data] = [msgJson["kind"], msgJson["data"]]

          switch type
            when "CurrentMap"
              [my_body, others_body, cur_map] = [data["your_body"], data["others_body"], data["map"]["content"]]
              ms.setMap(cur_map)
              ms.addMe(my_body["id"]["id"], [my_body["pos"]["x"], my_body["pos"]["y"]])
              ms.setPlayer(el["id"]["id"], [el["pos"]["x"], el["pos"]["y"]]) for el in others_body

            when "Player_Move" then ms.setPlayer(data["id"]["id"], [data["pos"]["x"], data["pos"]["y"]])
            when "Player_Join" then ms.setPlayer(data["id"]["id"], [data["pos"]["x"], data["pos"]["y"]])
            when "Player_Quit" then ms.rmPlayer(data["id"]["id"])
            when "YouQuit" then ms.init_empty()
            when "YouJump" then document.location.href = data["url"]

            else
              console.log "no handler for that : "
              console.log evt.data

        ws.onerror = (evt) -> console.log evt
        
        ms.setCanvasFct (id, [x, y]) ->
            res =  JSON.stringify({kind:'Me_Move', data: { pos : {x:x, y:y} }})
            console.log("sending : #{res}")
            ws.send(res)
        
        i = 0
        
        $.processing = processing
        
        autoMove([], [["right", 1000], ["up", 1000]])
        
    catch error
        console.log error
        

	
