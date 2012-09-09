
###
###


define(['module', 'log', 'heart', 'external/kinetic', 'play_map'], (module, log, heart, kinetic, play_map) ->

  MAP_MSG =
    MAP_CONTENT : "map:init"

    PLAYER_JOIN : "map:player_join"
    PLAYER_STATUS : "map:player_status"
    PLAYER_QUIT : "map:player_quit"

  class MapPlayer extends play_map.MapElement

  class MapPlayers extends Backbone.Collection
    model : MapPlayer

  class MapUsersModel extends Backbone.Model
    defaults:
      players : new MapPlayers()
      mainPlayerId : null

    initialize: () ->
      _.bindAll @
      heart.on(MAP_MSG.MAP_CONTENT, @setMapContent)
      heart.on(MAP_MSG.PLAYER_JOIN, @playerAdd)
      heart.on(MAP_MSG.PLAYER_STATUS, @playerState)
      heart.on(MAP_MSG.PLAYER_QUIT, @playerQuit)

    ###############
    # response to msg
    ###############
    setMapContent : (my_body, others_body, cur_map) ->
      log.debug "set map content for map users"
      id = my_body["id"]["id"]
      [x, y] = [my_body["pos"]["x"], my_body["pos"]["y"]]
      @set('mainPlayer', new MapPlayer({posX : x, posY: y, _id:id}))

      @set('mainPlayerId', my_body["id"]["id"])

      @playerQuit({id : p.id}) for p in @get('players').toArray()
      @playerAdd(el["id"], el["pos"]) for el in others_body

    isMain: (id) -> id['id'] == @get('mainPlayerId')

    playerAdd: (id, pos) ->
      if @isMain(id) then return

      player = new MapPlayer({ id : id["id"], x : pos['x'], y : pos['y'] })
      @get('players').add(player)

      @trigger("add:player", player)

    playerState: (id, pos) ->
      if @isMain(id) then return

      log.debug "id got : ", id
      log.debug @get('players')

      player = @get('players').get(id['id'])

      if not player
        @playerAdd(id, pos)
        return

      player.setPos(pos['x'], pos['y'])

      @trigger("change:player", player)

    playerQuit: (id) ->
      if @isMain(id) then return

      player = @get('players').get(id['id'])
      @get('players').remove(player)

      @trigger("remove:player", player)




  class MapUsersView extends Backbone.View
    players : {}
    mapX : null
    mapY : null
    step : module.config().view_step

    mapMain : null
    layer : new Kinetic.Layer()

    initialize: ->
      _.bindAll @
      @mapMain = @options['mapMain']
      @mapMain.addLayer(@layer)

      @model.bind("add:player", @addPlayer)
      @model.bind("remove:player", @rmPlayer)
      @model.bind("change:player", @changePlayer)

      #@model.bind('change:state', @statusChanged)
      #@model.bind('change:mainPlayer', @render)

    addPlayer : (player) ->
      [x, y] = player.getPos()

      mel = new Kinetic.Rect({
        x : x * @step,
        y : y * @step,
        width : @step,
        height : @step,
        fill : "green",
        strokeWidth : 0
      })

      @layer.add(mel)
      @players[player.id] = mel
      @render()

    rmPlayer : (player) ->
      @layer.remove(@players[player.id])
      delete @players[player.id]
      @render()

    changePlayer : (player) ->
      el = @players[player.id]
      [x, y] = player.getPos()
      el.setX(x * @step)
      el.setY(y * @step)
      @render()

    render : () ->
      @layer.draw()
      @mapMain.render()



  return {
    MapUsersModel : MapUsersModel
    MapUsersView : MapUsersView
  }

)


