package models.ExternalLink

/********************
 * Create a Link as Actor througth external elements
 */

import scala.actors._
import models.msg_json.MSG_JSON
import play.api.Play.current //to keep current application in scope


object ExternalLink {
  case class CONNECTION_END(ex : Exception)
  case class FROM_LINK(o : Any)
}

abstract trait ExternalLink extends Actor {}


object WebServicePassif { class WebServiceClosed() extends Exception }

class WebServicePassif(actor : Actor) extends ExternalLink {
  import play.api.libs.json._
  import play.api.libs.iteratee._
  import play.api.libs.concurrent._


  val enumChannel = Enumerator.imperative[JsValue]()
  val iteChannel = Iteratee.foreach[JsValue]{ event =>
    println("from client : ", event)
    actor ! ExternalLink.FROM_LINK(MSG_JSON.fromJson(event))
  }.mapDone { _ =>
    println("from client, end of all")
    actor ! ExternalLink.CONNECTION_END(new WebServicePassif.WebServiceClosed())
  }

  def act = loop {
    receive {
      case e : Any => enumChannel.push(MSG_JSON.toJsonPlay(e))
    }
  }

  def getPromise(): Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = Akka.future{ (iteChannel, enumChannel) }
}



object WebServiceActif { class WebServiceClosed(code : Int, reason : String) extends Exception }

class WebServiceActif(actor : Actor, url: String) extends ExternalLink {

  val ws = org.jfarcand.wcs.WebSocket().open(url)

  ws.listener( new org.jfarcand.wcs.MessageListener{
    override def onMessage(message: String){
      actor ! ExternalLink.FROM_LINK(MSG_JSON.fromJson(message))
    }

    override def onClose(code : Int, reason : String){
      actor ! ExternalLink.CONNECTION_END(new WebServiceActif.WebServiceClosed(code, reason))
    }
  })

  def act = loop {
    receive {
      case e : Any => ws.send(MSG_JSON.toJson(e))

    }
  }
}




