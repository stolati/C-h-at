
step = 20
window.log = console.log

class SquarePlace
    constructor: (@pos_x, @pos_y) ->

    move: (x, y) ->
        @pos_x += x
        @pos_y += y




log "creating myCanvasFct"
ms = new MapSurface()

myCanvasFct = ([x, y]) -> 
listElems = {}
myId = ""

isInitalized = false

mapContent = null
mapSize = [1, 1]

moveTo = (where) ->
  return if not isInitalized
  
  [curX, curY] = listElems[myId]
  [maxX, maxY] = mapSize
  
  switch where
    when "left" then curX -= 1
    when "right" then curX += 1
    when "up" then curY -= 1
    when "down" then curY += 1
    else return
    
  curX = Math.max(0, curX)
  curY = Math.max(0, curY)
  curX = Math.min(curX, maxX - 1)
  curY = Math.min(curY, maxY - 1)
        
  listElems[myId] = [curX, curY] 
  log "myCanvasFct( [#{curX}, #{curY}] )"
  myCanvasFct( [curX, curY] )


autoMove = (moves, autoRest) => 
  console.log "autoMove(#{moves}, #{autoRest})"
  if moves.length == 0
    [move, time] = autoRest
  else
    [move, time] = moves[0]
    moves = moves[1..]
  
  moveTo(move)
  nextFct = () -> autoMove(moves, autoRest)
  setTimeout(nextFct, time)

myCanvas_draw = (cv) ->
    cv.setup = () ->
       cv.size(Math.floor($(window).width() / 1.5), Math.floor($(window).height() / 1.3) )
       cv.background(255)
       
    cv.draw = () ->
        [sizeX, sizeY] = mapSize
        cv.size(sizeX * step, sizeY * step)
        cv.background(255)
        cv.stroke(50)

        #drawing 5 per 5 cases lines
        for x in [1..cv.width] by step
            cv.line(x, 0, x, cv.height)	
        for y in [1..cv.height] by step
            cv.line(0, y, cv.width, y)

        return if not isInitalized
        #drawing map floor
        [w, h] = mapSize 
        for x in [0..w-1]
          for y in [0..h-1]
            switch mapContent[y][x]
              when "F"
                cv.fill(0, 0, 0)
              when "B"
                cv.fill(255, 255, 255)
                
            cv.rect(step * x + 1, step * y + 1, step, step)
            
        #printing of users
        for id, [x, y] of listElems
              if id == myId
                  cv.fill(255, 0, 0) # red
                  cv.stroke(100)
              else
                  cv.fill(0, 255, 0) # green
                  cv.stroke(255)
              cv.rect(step * x + 1, step * y + 1, step, step)
              
          
    cv.keyPressed = () ->
      switch cv.keyCode
        when 37 then moveTo("left")
        when 39 then moveTo("right")
        when 38 then moveTo("up")
        when 40 then moveTo("down")
        

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
            msgJson = JSON.parse(evt.data)
            [type, data] = [msgJson["type"], msgJson["data"]]
            #console.log evt.data
            switch type
              when "first_connect"
                map = data["map"]
                console.log data
                mapSize = [map["w"], map["h"]]
                mapContent = map["content"]
                
                me = data["me"]
                myId = me["id"]
                listElems[myId] = [me["x"], me["y"]]
                
                for el in data["other"]
                  listElems[el["id"]] = [el["x"], el["y"]]
                
                isInitalized = true
                
              when "move"
                listElems[data["id"]] = [data["x"], data["y"]]
                
              when "join"
                listElems[data["id"]] = [data["x"], data["y"]]
                
              when "quit"
                delete listElems[data["id"]]
                
              when "disconnected"
                window.listElems = {}
                myId = ""
                mapContent = null
                mapSize = [1, 1]
                isInitalized = false
                
              else
                console.log "no handler for that : "
                console.log evt.data

        ws.onerror = (evt) -> console.log evt
        
        myCanvasFct = ([x, y]) ->
            res =  JSON.stringify({type:'move', data: {id:myId, x:x, y:y}})
            console.log("sending : #{res}")
            ws.send(res)
        
        i = 0
        $("button").button().click( () ->
            console.log "click and send message"
            ws.send("click " + i)
            i += 1
            processing.exit()
        )
        
        $.processing = processing
        
        autoMove([], ["right", 1000])
        
    catch error
        console.log error
        

	
