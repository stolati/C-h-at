env_type = 'dev'
#env_type = 'prod'

#configuration of requirejs
requirejs.config
    baseUrl : 'assets/javascripts'
    enforceDefine : {'dev' : true, 'prod' : false}[env_type]
    paths :
        'external' : env_type
        'external/json2' : 'JSON2-js/json2'
        'log4javascript' : 'log4javascript/log4javascript' + {'dev' : '_uncompressed', 'prod' : ''}[env_type]
    shim :
        'external/json2' :
            exports : 'JSON'
        'external/underscore' :
            exports : '_'
        'external/backbone' :
            deps : ['external/underscore', 'external/jquery']
            exports : 'Backbone'
        'external/processing' :
            exports : 'Processing'
        'external/jquery' :
            exports : '$'
        'log4javascript' :
            exports : 'log4javascript'

    deps : ['external/json2', 'external/underscore', 'external/backbone'] #TODO puts modernizr as dependence (and remove json2)

    config :
      global :
        'env' : env_type
      log :
        'log_type' : {'dev' : 'firebug', 'prod' : 'none'}[env_type]
        'log_level' : {'dev' : 'ALL', 'prod' : 'WARN'}[env_type]
      heart :
        msg_rename :
          'init' : 'login:init'
      linkServer :
        to_serv :
          'login:connect(username, password)' : 'PlayerCredential'
        from_serv :
          'PlayerCredential' : 'login:connect(toto,titi,tutu)'




#start the main logic
define(['log', 'external/underscore', 'heart', 'linkServer', 'login'], (log, _, heart, login)->

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



