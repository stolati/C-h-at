###
Login interface
define events : login:init, login:success, login:failed
###

#TODO can have a list of users while typing
#TODO have a stuff saying processing while waiting for the server
#TODO have a short window saying connected while connected (like staying 5 seconds)

define(['log', 'heart'], (log, heart) ->


  log.info "login bebing"

  LOGIN_STATUS =
    BEFORE_INIT : "BEFORE_INIT"
    INIT        : "INIT"
    WAIT_CHECK  : "WAIT_CHECK"
    SUCCESS     : "SUCCESS"
    FAIL        : "FAIL"

  LOGIN_MSG =
    INIT : "login:init"
    SUCCESS : "login:success"
    FAILED : "login:failed"
    CONNECT : "login:connect"


  class LoginModel extends Backbone.Model
    defaults:
      name : "toto"
      heart : heart
      state : LOGIN_STATUS.BEFORE_INIT
      msg : null

    initialize: () ->
      _.bindAll @
      log.info @.set
      heart.on("login:init", _.bind(@setState, @, LOGIN_STATUS.INIT) )
      heart.on("login:success", _.bind(@setState, @, LOGIN_STATUS.SUCCESS) )
      heart.on("login:failed", _.bind(@setState, @, LOGIN_STATUS.FAIL) )

    setState: (state, msg = null) -> @set({'state': state, 'msg' : msg} )
    connect: (name) ->
      if @get('state') not in [LOGIN_STATUS.INIT, LOGIN_STATUS.FAIL] then return
      log.info "launching on #{name}"
      @setState(LOGIN_STATUS.WAIT_CHECK)
      heart.trigger("login:connect", name)


  class LoginView extends Backbone.View
    className : 'div'

    template : _.template('
      <div   class="login_err_div" id="login_err_div"   class="err_msg"><%= msg %></div>
      <input class="login_input_txt" id="login_input_txt" type="text"   value="<%= name %>"/>
      <input class="login_ok_btt" id="login_ok_btt"    type="button" value="connect"/>
    ')

    events :
      'click .login_ok_btt' : "connect"

    initialize: ->
      _.bindAll @
      @model.bind('change', @render)

      #$('body').append(@$el)

    render : () ->
      log.info "render LoginView"
      log.info "state of the model : " + @model.get('state')

      @$el.html( @template({name : @model.get("name"), msg : @model.get("msg") || ""} ) )

      if @$el.parents().size() == 0 then $('body').append(@$el)

      switch @model.get('state')
        when LOGIN_STATUS.BEFORE_INIT then @remove()
        when LOGIN_STATUS.INIT
          @$el.show()
          @setDisable(false)

        when LOGIN_STATUS.SUCCESS then @remove()

        when LOGIN_STATUS.WAIT_CHECK
          @$el.show()
          @setDisable(true)
        when LOGIN_STATUS.FAIL then @$el.show().prop('disabled', false)
        else
          throw new Error("Status [#{@model.get('state')}] not known")

    setDisable : (status) -> @$('.login_input_txt, .login_ok_btt').prop('disabled', status)


    connect : () -> @model.connect( @$('#login_input_txt') )


  lm = new LoginModel()
  lv = new LoginView({model: lm})

  log.info("login end")
  return {
    LoginModel : LoginModel
    LoginView : LoginView
    lm : lm
    lv : lv
    LOGIN_MSG, LOGIN_MSG
  }
)


