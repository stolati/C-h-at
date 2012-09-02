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
            exports : '$'
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
          'login:success' : 'canvas:getMap'

      linkServer :
        to_serv :
          'login:connect(username, password)' : 'PlayerCredential'
          'login:get_list()' : 'GetPlayerList'
          'canvas:getMap()' : 'Ask_Map'

        from_serv :
          'KOPlayerCredential' : 'login:failed(msg)'
          'OKPlayerCredential' : 'login:success()'
          'PlayerList' : 'login:list(content)'


#start the main logic
define(['log', 'heart', 'linkServer', 'login'], (log, heart, linkServ, login)->

  log.info "login : ", login

  heart.trigger('init')
  heart.trigger('login:connect', 'toto', '1234')

)

###
log levels :
   trace(msg)
   debug(msg)
   info(msg)
   warn(msg)
   fatal(msg)
###



