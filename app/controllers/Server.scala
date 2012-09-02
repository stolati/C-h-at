package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.ExternalLink.WebServicePassif


object Server extends Controller {

  def ws = WebSocket.async[String]{ request =>

    val ca = new ServerLink()
    val cl = new WebServicePassif(ca)
    ca.setWs(cl)

    cl.start()
    ca.start()

    cl.getPromise()
  }

}


