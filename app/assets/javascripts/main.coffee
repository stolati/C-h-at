env_type = 'dev'
#env_type = 'prod'

requirejs.onError = (err) ->
  console.log "=====> err" + err
  console.log err



#configuration of requirejs
requirejs.config
    baseUrl : 'assets/javascripts'
    enforceDefine : {'dev' : true, 'prod' : false}[env_type]
    paths :
        'external' : env_type
        'external/json2' : 'JSON2-js/json2'
        'log4javascript' : 'log4javascript/log4javascript' + {'dev' : '_uncompressed', 'prod' : ''}[env_type]
        'external/jquery' : 'jquery_ui/js/jquery-1.7.2.min'
        'external/jquery_ui' : 'jquery_ui/js/jquery-ui-1.8.23.custom.min'
    shim :
        'external/json2' : { exports : 'JSON' }
        'external/underscore' : { exports : '_' }
        'external/backbone' :
            deps : ['external/underscore', 'external/jquery']
            exports : 'Backbone'
        'external/processing' : { exports : 'Processing' }
        'external/jquery' : { exports : '$' }
        'external/jquery_ui':
            deps : ['external/jquery']
            exports : 'window' #dummy
        'external/kinetic':
            exports : 'Kinetic'
        'log4javascript' : { exports : 'log4javascript' }

    deps : ['external/jquery_ui', 'external/backbone', 'external/json2'] #TODO puts modernizr as dependence (and remove json2)

    config :
      global :
        'env' : env_type
      log :
        'log_type' : {'dev' : 'firebug', 'prod' : 'none'}[env_type]
        'log_level' : {'dev' : 'ALL', 'prod' : 'WARN'}[env_type]
      heart :
        msg_rename :
          'init' : 'login:init'

      play_map : {view_step : 15}
      map_floor : {view_step : 15}
      map_player : {view_step : 15}
      map_users : {view_step : 15}
      map_beam : {view_step : 15}

      linkServer :
        to_serv :
          'login:connect(username, password)' : 'PlayerCredential'
          'login:get_list()' : 'GetPlayerList'
          'map:me_moving(pos)' : 'Me_Move'
          'login:connect_with_id(id)' : 'PlayerJumpingId'
          'beam:launch(angle)' : 'BeamLaunch'

        from_serv :
          'KOPlayerCredential' : 'login:failed(msg)'
          'OKPlayerCredential' : 'login:success()'
          'YouJump' : 'login:change_url(url)'
          'PlayerList' : 'login:list(content)'
          'CurrentMap' : 'map:init(your_body, others_body, map)'
          'Player_Status' : 'map:player_status(id, pos)'
          'Player_Join' : 'map:player_join(id, pos)'
          'Player_Quit' : 'map:player_quit(id)'


#start the main logic
define(['log', 'heart', 'linkServer', 'login', 'play_map', 'map_floor', 'map_player', 'map_users', 'map_beam'],
(log, heart, linkServ, login, play_map, map_floor, map_player, map_users, map_beam)->

  log.info "login : ", login



  pmm = new play_map.PlayMapModel()
  pmv = new play_map.PlayMapView({model:pmm})

  mfm = new map_floor.MapFloorModel()
  new map_floor.MapFloorView({model:mfm, mapMain: pmv})

  mum = new map_users.MapUsersModel()
  new map_users.MapUsersView({model:mum, mapMain : pmv})

  mpm = new map_player.MapPlayerModel()
  new map_player.MapPlayerView({model:mpm, mapMain : pmv})

  #mbm = new map_beam.MapBeamModel()
  #new map_beam.MapBeamView({model : mbm, mapMain : pmv})

  heart.trigger('init')

  #from there, it's for dev purpose
  #heart.trigger('login:connect', 'toto', 'toto')

  #heart.on('map:player_join', ()->
  #  _.defer( ()->
  #    heart.trigger('map:me_moving', {_t:'Position', x : 16.5, y : 6 })
  #  )
  #)

)

###
log levels :
   trace(msg)
   debug(msg)
   info(msg)
   warn(msg)
   fatal(msg)
###



