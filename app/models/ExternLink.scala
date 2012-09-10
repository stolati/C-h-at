package models.ExternalLink

/********************
 * Create a Link as Actor througth external elements
 */

import scala.actors._
import models.msg_json._
import play.api.Play.current //to keep current application in scope


object ExternalLink {
  case class CONNECTION_END(ex : Exception)
  case class FROM_LINK(o : Any) //TODO say inside from which connection it's from (security issue)
}

abstract trait ExternalLink extends Actor {}


object WebServicePassif { class WebServiceClosed() extends Exception }

class WebServicePassif(actor : Actor) extends ExternalLink {
  import play.api.libs.iteratee._
  import play.api.libs.concurrent._

  val enumChannel = Enumerator.imperative[String]()
  val iteChannel = Iteratee.foreach[String]{ event =>
    println("ExternalLink.WebServicePassif msg event = ", event)
    actor ! ExternalLink.FROM_LINK(Transform.fromJson(event))
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
        enumChannel.push(Transform.toJson(e))
    }
  }

  def getPromise(): Promise[(Iteratee[String,_],Enumerator[String])] = Akka.future{ (iteChannel, enumChannel) }
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
      actor ! ExternalLink.FROM_LINK(Transform.fromJson(message))
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
          ws.send(Transform.toJson(e))
      }
    }
  }
}




