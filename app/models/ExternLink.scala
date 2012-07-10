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
    println("ExternalLink.WebServicePassif msg event = ", event)
    actor ! ExternalLink.FROM_LINK(MSG_JSON.fromJson(event))
  }.mapDone { _ =>
    println("ExternalLink.WebServicePassif close event")
    actor ! ExternalLink.CONNECTION_END(new WebServicePassif.WebServiceClosed())
  }

  def act = loop {
    receive {
      case 'quit =>
        enumChannel.close()
        exit
      case e : Any =>
        println(this, "from server to client => ", e)
        enumChannel.push(MSG_JSON.toJsonPlay(e))
    }
  }

  def getPromise(): Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = Akka.future{ (iteChannel, enumChannel) }
}



object WebServiceActif { class WebServiceClosed(code : Int, reason : String) extends Exception }

class WebServiceActif(actor : Actor, url: String) extends ExternalLink {

  val ws = org.jfarcand.wcs.WebSocket().open(url)
  val me = this

  ws.listener(new org.jfarcand.wcs.MessageListener{

    override def onOpen {
      println(this, "WebServiceActif : onOpen")
      me ! 'wake_up
    }

    override def onMessage(message: Array[Byte]) { onMessage(new String(message))  }


    override def onMessage(message: String){
      print("message for wsActif : ", message)
      actor ! ExternalLink.FROM_LINK(MSG_JSON.fromJson(message))
    }

    override def onClose(code : Int, reason : String){
      print("onClose for wsActif : ", reason)
      actor ! ExternalLink.CONNECTION_END(new WebServiceActif.WebServiceClosed(code, reason))
    }
  })


  def act = {
    var opened = false
    loop {

      react {
        case 'wake_up => opened = true
        case 'quit =>
          ws.close
          exit
        case e : Any if opened =>
          println("sending from websa stuffs", e)
          ws.send(MSG_JSON.toJson(e))
      }
    }
  }
}




