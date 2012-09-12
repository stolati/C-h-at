
###
Map interface
define events : login:init, login:success, login:failed
###

#TODO can have a list of users while typing
#TODO have a stuff saying processing while waiting for the server
#TODO have a short window saying connected while connected (like staying 5 seconds)

define(['module', 'log', 'heart', 'external/kinetic', 'play_map'], (module, log, heart, kinetic, play_map) ->


  MAP_MSG =
    MAP_CONTENT : "map:init"


  class MapElement extends play_map.MapElement
    defaults:
      color : 'white'

    setPos : (x, y) -> @set({posX:x, posY:y})
    getPos : () -> [@get("posX"), @get("posY")]


  class MapWall extends MapElement

  class MapWalls extends Backbone.Collection
    model : MapWall



  class MapFloorModel extends Backbone.Model
    defaults:
      mapSize : [1, 1]
      mapWalls : new MapWalls


    initialize: () ->
      _.bindAll @
      heart.on(MAP_MSG.MAP_CONTENT, @setMapContent)

    setMapContent : (my_body, others_body, cur_map) ->
      data = cur_map['content']

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

      @trigger('change:mapWalls', @get('mapWalls'))



  class MapFloorView extends Backbone.View
    mapX : null
    mapY : null
    step : module.config().view_step

    mapMain : null
    el : new Kinetic.Layer()

    initialize: ->
      _.bindAll @
      @mapMain = @options['mapMain']
      @model.bind('change:mapWalls', @changeView)
      @mapMain.addLayer(@el)

    changeView: (ob, walls, option) ->
      @el.clear()


      [sizeX, sizeY] = @model.get('mapSize')
      [mapX, mapY] = [sizeX * @step, sizeY * @step]
      @mapMain.setSize([mapX, mapY])


      @el.add(new Kinetic.Rect({
        x : 0,
        y : 0,
        width : mapX,
        height : mapY,
        fill : "black",
        strokeWidth: 0
      }))

      #drawing 5 per 5 cases lines
      for x in [0..sizeX]
          @el.add(new Kinetic.Line({
            points : [x*@step, 0, x*@step, mapY],
            stroke : "white",
            strokeWidth: 1
          }))
      for y in [0..sizeY]
          @el.add(new Kinetic.Line({
            points : [0, y*@step, mapX, y*@step],
            stroke : "white",
            strokeWidth: 1
          }))


      for e in @model.get("mapWalls").map( (e) -> e )
        @el.add(play_map.mapElement2view(e, @step))

      @mapMain.render()


  return {
    MapFloorModel : MapFloorModel
    MapFloorView : MapFloorView
  }

)


