package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import models._
import models.ClientLink._


object ServerWS extends Controller {

  def index = Action {
    println("trying it")

    val conn = Servers.getServerLink("serv01")
    conn.push("toto_test01")


    println("end trying")
    Ok(views.html.canvas("!!! quit me !!!"))
  }

  def ws = WebSocket.async[JsValue]{ request =>

    val cl = new WSClientLink()

    cl.setEnd { () => println("end of the server connection") }
    cl.setSend( println("Receiving : ", _, _) )

    cl.getPromise()
  }


}

