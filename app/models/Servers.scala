package models

import org.jfarcand.wcs._
import models.ClientLink._
import play.api.libs.json._


object Servers {

  val serv_addr = Map(
    "S1" -> "ws://localhost:9000/serv",
    "localhost" -> "ws://localhost:9000/serv"
  )

  //TODO : don't create a connection each time, use a pool
  //TODO : getServerLink should be an actor

  def getServerLink(name : String) = {
    val url = serv_addr(name)
    new ServerLink(url)
  }


  class ServerLink(url: String) extends ClientLink {

      val ws = WebSocket().open(url)

      ws.listener( new MessageListener{
        override def onMessage(message: String){
          println("go message : ", message)
          println("go message : ", message.toString())
          val (kind, data) = js2send(Json.parse(message))
          sendFct(kind, data)
        }

        override def onClose(code : Int, reason : String){
          endFct()
        }
      })

      var sendFct : (String, JsValue) => Unit = (k : String, d : JsValue) => {}
      var endFct : () => Unit = () => {}

      override def push(kind : String, data : JsValue = JsNull) = ws.send(push2js(kind, data).toString())
      override def setSend(f : (String, JsValue) => Unit) = {sendFct = f}
      override def setEnd(f : () => Unit) = {endFct = f}
  }

}


