
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
    MAP_INIT : "map:init"
    MAP_QUIT : "map:quit"


  class MapElement extends Backbone.Model
    defaults:
      posX : null
      posY : null
      color : null

    setPos : (x, y) -> @set({posX:x, posY:y})
    getPos : () -> [@get("posX"), @get("posY")]


  class PlayMapModel extends Backbone.Model
    defaults:
      state : MAP_STATUS.NOT_MAP

    initialize: () ->
      _.bindAll @
      heart.on(MAP_MSG.MAP_INIT, _.bind(@set, @, 'state', MAP_STATUS.ON_MAP))
      heart.on(MAP_MSG.MAP_QUIT, _.bind(@set, @, 'state', MAP_STATUS.NO_MAP))


  #view part
  mapElement2view = (mapElement, step) ->
    [x, y] = mapElement.getPos()
    new Kinetic.Rect({
      x: step * x + 1,
      y : step * y + 1,
      width : step,
      height : step,
      fill : mapElement.get('color'),
      strokeWidth : 0
    })



  class PlayMapView extends Backbone.View
    className : 'div'
    stage : null

    initialize: ->
      _.bindAll @
      @model.bind('change:state', @statusChanged)
      @stage = new Kinetic.Stage({container : @el})
      $('body').append(@$el) # if @$el.parents().size() == 0

    addLayer: (layer) ->
      @stage.add(layer)

    statusChanged: (ob, status, option) ->

      switch status
        when MAP_STATUS.NO_MAP then @$el.hide()
        when MAP_STATUS.ON_MAP then @$el.show()

    setSize : ([sizeX, sizeY]) -> @stage.setSize(sizeX, sizeY)
    render : () -> @stage.draw()


  return {
    PlayMapModel : PlayMapModel
    PlayMapView : PlayMapView
    pos2Position : pos2Position
    Color : Color
    MapElement : MapElement
    mapElement2view : mapElement2view
  }

)



