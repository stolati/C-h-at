
step = 20
window.log = console.log

Colors = #enumerator of rgb
  white : [255, 255, 255]
  black : [0  , 0  , 0  ]
  green : [0  , 255, 0  ]
  red :   [255, 0  , 0  ]
  blue :  [0  , 0  , 255]


class SquarePlace
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

class MapSurface
  constructor: () ->
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
  
  setMap: (data, [w, h]) ->
    @mapSize = [w, h]
    @mapContent = data
    @isInit = true

  moveAction: (where) ->
    return if not @isInit
    
    [curX, curY] = @listElems[@id]
    [maxX, maxY] = @mapSize
  
    switch where
      when "left"  then curX -= 1
      when "right" then curX += 1
      when "up"    then curY -= 1
      when "down"  then curY += 1
      else return
    
    curX = Math.min( Math.max(0, curX), maxX - 1)
    curY = Math.min( Math.max(0, curY), maxY - 1)
        
    @listElems[@id] = [curX, curY] 
    log "myCanvasFct( [#{curX}, #{curY}] )"
    @canvasFct(@id, [curX, curY])

  getAt: ([x, y]) ->
    return "None" if not @isInit
    
    res = switch @mapContent[y][x]
      when "F" then "Floor"
      when "B" then "Block"

    for el_id, [el_x, el_y] of @listElems
      if x != el_x or y != el_y then continue
      if el_id == @id then return "Me"
      return "Other_player"
    
    res
  
  setPlayer: (id, [x, y]) -> @listElems[id] = [x, y]
  rmPlayer: (id) -> delete @listElems[id]
  addMe: (id, [x, y]) -> [@id, @listElems[id]] = [id, [x, y]]

  
ms = new MapSurface()



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

myCanvas_draw = (cv) ->
    cv.setup = () ->
       cv.size(Math.floor($(window).width() / 1.5), Math.floor($(window).height() / 1.3) )
       cv.background(255)

    cv.draw = () ->
        [sizeX, sizeY] = ms.mapSize
        cv.size(sizeX * step, sizeY * step)
        cv.background(255)
        cv.stroke(50)
        
        #drawing 5 per 5 cases lines
        for x in [1..cv.width] by step
            cv.line(x, 0, x, cv.height)	
        for y in [1..cv.height] by step
            cv.line(0, y, cv.width, y)

        #drawing map floor
        [w, h] = ms.mapSize

        for x in [0..w-1]
          for y in [0..h-1]
            [r, g, b] = switch ms.getAt([x, y])
              when "None" then Colors.blue
              when "Floor" then Colors.black
              when "Block" then Colors.white
              when "Me" then Colors.red
              when "Other_player" then Colors.green
              else Colors.blue
            
            cv.fill(r, g, b)
            cv.rect(step *x + 1, step * y + 1, step, step)
          
    cv.keyPressed = () ->
      switch cv.keyCode
        when 37 then ms.moveAction("left")
        when 39 then ms.moveAction("right")
        when 38 then ms.moveAction("up")
        when 40 then ms.moveAction("down")
        

$(document).ready ->
    try
        canvas = document.getElementById "myCanvas"
        console.log canvas
        processing = new Processing($("canvas")[0], myCanvas_draw)
        
        console.log "toto"
        
        wsUri = document.location.host
        wsUri = "ws://#{wsUri}/ws"
        
        ws = new WebSocket(wsUri)
        ws.onopen = (evt) -> console.log evt
        ws.onclose = (evt) -> console.log evt
        ws.onmessage = (evt) ->
            console.log "reception of : ", evt.data
            msgJson = JSON.parse(evt.data)
            [type, data] = [msgJson["type"], msgJson["data"]]
            
            switch type
              when "first_connect"
                [map, me, other] = [data["map"], data["me"], data["other"]]
                ms.setMap(map["content"], [map["w"], map["h"]])
                ms.addMe(me["id"], [me["x"], me["y"]])
                ms.setPlayer(el["id"], [el["x"], el["y"]]) for el in other
              when "setPlayer" then ms.setPlayer(data["id"], [data["x"], data["y"]])
              when "join"then ms.setPlayer(data["id"], [data["x"], data["y"]])
              when "quit" then ms.rmPlayer(data["id"])
              when "disconnected" then ms.init_empty()
              else
                console.log "no handler for that : "
                console.log evt.data

        ws.onerror = (evt) -> console.log evt
        
        ms.setCanvasFct (id, [x, y]) ->
            res =  JSON.stringify({type:'move', data: {id:id, x:x, y:y}})
            console.log("sending : #{res}")
            ws.send(res)
        
        i = 0
        
        $.processing = processing
        
        autoMove([], [["right", 1000], ["up", 1000]])
        
    catch error
        console.log error
        

	
