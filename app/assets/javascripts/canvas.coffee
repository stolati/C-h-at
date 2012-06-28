
step = 20
window.log = console.log

class SquarePlace
    constructor: (@pos_x, @pos_y) ->

    move: (x, y) ->
        @pos_x += x
        @pos_y += y


log "creating myCanvasFct"
myCanvasFct = ([x, y]) -> 
window.listElems = {}
myId = ""

mapContent = null
mapSize = [0, 0]

myCanvas_draw = (cv) ->
    cv.setup = () ->
       cv.size(Math.floor($(window).width() / 1.5), Math.floor($(window).height() / 1.3) )
       cv.background(255)
       
       @maxX = Math.floor(cv.width / step) - 1
       @maxY = Math.floor(cv.height / step) - 1
       
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

        #drawing map floor            
        [w, h] = mapSize 
        for x in [0..w-1]
          for y in [0..h-1]
            switch mapContent[y][x]
              when "F"
                cv.fill(0, 0, 0)
              when "B"
                cv.fill(0, 255, 255)
                
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
        [curX, curY] = listElems[myId]
        
        if cv.keyCode is 37 #left
            curX -= 1
        if cv.keyCode is 39 #right
            curX += 1
        if cv.keyCode is 38 #up
            curY -= 1
        if cv.keyCode is 40 #down
            curY += 1

        
        
        curX = Math.max(0, curX)
        curY = Math.max(0, curY)
        curX = Math.min(@maxX, curX)
       	curY = Math.min(@maxY, curY)
        
        listElems[myId] = [curX, curY] 
        log "myCanvasFct( [#{curX}, #{curY}] )"
        myCanvasFct( [curX, curY] )
        
        #console.log "#{@curX}x#{@curY} (on max : #{@maxX}x#{@maxY} )"


$(document).ready ->
    try
        canvas = document.getElementById "myCanvas"
        console.log canvas
        processing = () -> new Processing($("canvas")[0], myCanvas_draw)
        
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
                mapSize = [map["w"], map["h"]]
                mapContent = map["content"]
                
                me = data["me"]
                myId = me["id"]
                listElems[myId] = [me["x"], me["y"]]
                
                for el in data["other"]
                  listElems[el["id"]] = [el["x"], el["y"]]
                
                processing = processing()
              when "move"
                listElems[data["id"]] = [data["x"], data["y"]]
                
              when "join"
                listElems[data["id"]] = [data["x"], data["y"]]
                
              when "quit"
                delete listElems[data["id"]]
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
        
    catch error
        console.log error
        

	