
step = 10

class SquarePlace
	constructor: (@pos_x, @pos_y) ->
		
	move: (x, y) ->
		@pos_x += x
		@pos_y += y
		

myCanvasFct = ([x, y]) -> 


myCanvas_draw = (cv) ->
	cv.setup = () ->
		cv.size(Math.floor($(window).width() / 1.5), Math.floor($(window).height() / 1.3) )
		cv.background(0)
		
		@curX = 2
		@curY = 3
		
		@maxX = Math.floor(cv.width / step) - 1
		@maxY = Math.floor(cv.height / step) - 1
		
		myCanvasFct( [@curX, @curY] )

	cv.draw = () ->
		cv.background(0)

		cv.stroke(255)
		
		#drawing 5 per 5 cases lines
		for x in [1..cv.width] by step
			cv.line(x, 0, x, cv.height)	
		for y in [1..cv.height] by step
			cv.line(0, y, cv.width, y)

		cv.stroke(100)
		cv.fill(204, 102, 0)
		cv.rect(step * @curX + 1, step * @curY + 1, step, step)
		
		#for y in [1..cv.height] by step
		#		cv.line(30, 20, 85, 20)

	cv.keyPressed = () ->
		if cv.keyCode is 37 #left
			@curX -= 1
		if cv.keyCode is 39 #right
			@curX += 1
		if cv.keyCode is 38 #up
			@curY -= 1
		if cv.keyCode is 40 #down
			@curY += 1
		
		@curX = Math.max(0, @curX)
		@curY = Math.max(0, @curY)
		@curX = Math.min(@maxX, @curX)
		@curY = Math.min(@maxY, @curY)
		
		myCanvasFct( [@curX, @curY] )
		
		#console.log "#{@curX}x#{@curY} (on max : #{@maxX}x#{@maxY} )"


$(document).ready ->
	try
		canvas = document.getElementById "myCanvas"
		console.log canvas
		processing = new Processing($("canvas")[0], myCanvas_draw)
		
		console.log "toto"
		
		wsUri = "ws://localhost:9000/ws"
		
		ws = new WebSocket(wsUri)
		ws.onopen = (evt) -> console.log evt
		ws.onclose = (evt) -> console.log evt
		ws.onmessage = (evt) -> console.log "new message :" + JSON.parse(evt.data)
		ws.onerror = (evt) -> console.log evt
		
		myCanvasFct = ([x, y]) ->
			ws.send( JSON.stringify({'x':x, 'y':y}) )
		
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
		

	