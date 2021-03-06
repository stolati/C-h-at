###
Login interface
define events : login:init, login:success, login:failed
###

#TODO can have a list of users while typing
#TODO have a stuff saying processing while waiting for the server
#TODO have a short window saying connected while connected (like staying 5 seconds)

define(['log', 'heart'], (log, heart) ->

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
    LIST : "login:list"
    ASK_LIST : "login:get_list"
    CHANGE_URL : 'login:change_url'


  class LoginModel extends Backbone.Model
    defaults:
      name : "" #TODO remove and set to nothing (or get the cookie info)
      heart : heart
      state : LOGIN_STATUS.BEFORE_INIT
      listNames : []
      msg : null

    initialize: () ->
      _.bindAll @
      heart.on(LOGIN_MSG.INIT, _.bind(@setState, @, LOGIN_STATUS.INIT) )
      heart.on(LOGIN_MSG.SUCCESS, _.bind(@setState, @, LOGIN_STATUS.SUCCESS) )
      heart.on(LOGIN_MSG.FAILED, _.bind(@setState, @, LOGIN_STATUS.FAIL) )
      heart.on(LOGIN_MSG.LIST, _.bind(@addToList, @) )
      heart.on(LOGIN_MSG.CHANGE_URL, _.bind(@change_url, @))

    change_url: (url) ->
      window.location.href = url

    addToList: (listNames) ->
      @set("listNames", @get("listNames").concat(listNames))

    setState: (state, msg = null) -> @set({'state': state, 'msg' : msg} )
    connect: (name, pass) ->
      if @get('state') not in [LOGIN_STATUS.INIT, LOGIN_STATUS.FAIL] then return
      @setState(LOGIN_STATUS.WAIT_CHECK)
      heart.trigger("login:connect", name, pass)

    haveConnectionId : (id) ->
      @setState(LOGIN_STATUS.WAIT_CHECK)
      heart.trigger("login:connect_with_id", {'_t' : 'Id', 'id' : id})


  class LoginView extends Backbone.View
    className : 'div'

    #TODO do the focus and enter stuff
    template : _.template('
      <div   class="login_err_div" class="err_msg"><%= msg %></div>
      <input class="login_input_txt" type="text"  value="<%= name %>"/><br/>
      <input class="login_input_pass" type="password"  value=""/><br/>
      <input class="login_ok_btt" type="button" value="connect"/>
    ')

    events :
      'click .login_ok_btt' : "connect"

    initialize: ->
      _.bindAll @
      @model.bind('change:state', @render)
      @model.bind('change:listNames', @updateList)
      @$el.html( @template({name : @model.get("name"), msg : @model.get("msg") || ""} ) )

      @$('.login_input_txt').autocomplete({
         minLength : 1,
         source : ['toto', 'tutu', 'titi']
      })


      #loop through search part of the url
      for keyVal in window.location.search.substr(1).split("&")
        [key, val] = keyVal.split("=")
        key = unescape(key)
        val = (if val? then unescape(val) else "")

        if key == "id"
          @model.haveConnectionId(val)


    render : () ->
      state = @model.get('state')

      switch state
        when LOGIN_STATUS.BEFORE_INIT then @setViewStatus(false, false, true)
        when LOGIN_STATUS.SUCCESS then @setViewStatus(false, false, false)
        when LOGIN_STATUS.INIT then @setViewStatus(true, true, true)
        when LOGIN_STATUS.WAIT_CHECK then @setViewStatus(true, false, true)
        when LOGIN_STATUS.FAIL then @setViewStatus(true, true, true)

        else
          throw new Error("Status [#{@model.get('state')}] not known")

    setViewStatus : (toShow, toEnable, toLink) ->
      if toShow then @$el.show() else @$el.hide()
      @$('.login_input_txt, .login_input_pass, .login_ok_btt').prop('disabled', not toEnable)

      if @$el.parents().size() == 0 and toLink then $('body').append(@$el)
      if @$el.parents().size() != 0 and not toLink then @remove()

    updateList : () ->
      @$('.login_input_txt').autocomplete({minLength : 1, source : @model.get('listNames')})

    connect : () -> @model.connect( @$('.login_input_txt').val(), @$('.login_input_pass').val() )


  lm = new LoginModel()
  lv = new LoginView({model: lm})

  return {
    LoginModel : LoginModel
    LoginView : LoginView
    lm : lm
    lv : lv
    LOGIN_MSG, LOGIN_MSG
  }
)


