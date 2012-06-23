package models

import akka.actor._
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.ask

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.Play.current

object MapRoom extends App {
  
  lazy val default = {
    val mapRoomActor = Akka.sytem.actorOf(Props[MapRoom])
    mapRoomActor
  }
  
  def join():Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    (default ? Join()).asPromise.map {
      case _ =>
        println("try to connect")
        val iteratee = Done[JsValue, Unit]((), Input.EOF)
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumrator.enumInput(Input.EOF))
      
        (iteratee, enumerator)
    }
    
    
    
  }
  
  
}

class MapRoom extends Actor {
  
    var players = List.empty[PushEnumerator[JsValue]]
    
    def receive = {
      
      case _ => {
        println("map room received something")
        sender ! Join()
      }
      
      
      
    }
    
  
  
}


case class Join()