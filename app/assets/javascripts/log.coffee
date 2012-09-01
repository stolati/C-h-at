###
do the configuration for the logger
and return the log object
possible configuration names : firebug alert dom pop_up
TODO add server as config name for logging to the server

possible level names :  ALL TRACE DEBUG INFO WARN ERROR FATAL OFF
###


define(['module', 'external/jquery', 'log4javascript'], (module, $, log4js) ->

  [log_level, log_type] = [module.config().log_level, module.config().log_type]

  if log_type == "none"
    log_level = "OFF"
    log_type = "alert"

  appender = switch log_type
    when "firebug" then new log4js.BrowserConsoleAppender()
    when "alert" then new log4js.AlertAppender()
    when "dom"
      $('body').append('<div id="log_div" name="log_div"/>')
      new log4js.InPageAppender("log_div")
    when "pop_up" then new log4js.PopUpAppender()
    else
      throw new Error("no log type [#{log_type}]")

  level = switch log_level
    when "ALL"   then log4js.Level.ALL
    when "TRACE" then log4js.Level.TRACE
    when "DEBUG" then log4js.Level.DEBUG
    when "INFO"  then log4js.Level.INFO
    when "WARN"  then log4js.Level.WARN
    when "ERROR" then log4js.Level.ERROR
    when "FATAL" then log4js.Level.FATAL
    when "OFF"   then log4js.Level.OFF
    else
      throw new Error("no log level [#{log_level}]")


  log = log4js.getLogger()
  log.addAppender(appender)
  log.setLevel(level)

  log.debug("log created with level '#{log_level}' to output '#{log_type}')")
  log

)

