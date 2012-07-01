package models

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.Play.current

import scala.util.Random
import java.util.ArrayList

import scala.actors._
import scala.actors.Actor._

import models.MapSurface._


object ClientActor {
  
  case class JsFromClient(kind : String, data : JsValue)
  case class ClientClosed()
  
  def sendJsValue(js : JsValue) = {
    
    
  }
  
  
  def join(name : String, pos : Option[(Int, Int)]):Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    
    val channel = Enumerator.imperative[JsValue]()
    val map = MapRoom.maps(name)
    val ca = new ClientActor(MapRoom.maps(name), channel)
    
    ca ! GoJoin(map, pos)
    
    val iteratee = Iteratee.foreach[JsValue]{ event =>
      val (kind, data) = ((event \ "type").as[String], (event \ "data"))
      printf("from client : %s = %s\n", kind, data)
      ca ! JsFromClient(kind, data)
    }.mapDone { _ => 
      ca ! ClientClosed()
    }
    
    ca.start()
    Akka.future{ (iteratee, channel) }
    
    //val speak = (curMap !? JoinInit(None))
    
    //speak match {
    //  case JoinInitRes(fci, channel) => 
    //    val id = fci.myBody.id
    // /   println("joined, with id : ", id)
    //    
    //    println("connected launched")
    //    // Create an Iteratee to consume the feed
    //    val iteratee = Iteratee.foreach[JsValue] { event =>
    //      receptionWS(event, id, curMap)
    //    }.mapDone { _ =>
    //      curMap ! Quit(id)
    //    }
    //    
    //    val msg = MapRoom.Msg("first_connect", FirstConnectionInfo.writes(fci))
    //    val channelWithFirstMsg = Enumerator[JsValue](msg).andThen(channel)
    //    Akka.future{ (iteratee, channelWithFirstMsg) }
    //}

  }
}


class ClientActor(var curMap : MapRoom, channel : PushEnumerator[JsValue]) extends Actor {

  var id : String = ""
  
  
  def toCli(kind : String, data : JsValue) = {
    val jsStruct = JsObject(Seq("type" -> JsString(kind), "data" -> data))
    printf("to client : %s\n", jsStruct)
    this.channel.push( jsStruct )
  }
   
  def act = {
    loop { receive {
      
      case GoJoin(cm, pos) =>
        if(curMap != cm || id != ""){
          this.toCli("disconnected", JsNull)
          curMap ! Quit(id)
        }
        
        this.curMap = cm
        this.curMap ! IJoin(pos)
        
      case fc : FirstConnectionInfo =>
          this.id = fc.myBody.id
          this.toCli("first_connect", FirstConnectionInfo.writes(fc))

      case HasJoined(b) => this.toCli("join", Body.writes(b))
      case SomeoneQuit(b) => this.toCli("quit", Body.writes(b))
      case Move(b) => this.toCli("setPlayer", Body.writes(b))

      case ClientActor.ClientClosed() =>
        curMap ! Quit(id)
        println("quitting")
        exit

      case ClientActor.JsFromClient("move", b) => curMap ! Move(Body.reads(b))
        
        
      
      case x : Any => println("ClientActor receive : ", x)
        
      }
    }
    
  }

}









object MapRoom {

  lazy val maps = {
    MapSurface.names.map { e =>
      val (s, ms) = e
      val myMap = new MapRoom(ms)
      myMap.start()
      s -> myMap
    }.toMap
  }
  
  lazy val random = new Random(12345)
  
  var id = 0
  def getNextId = { id += 1 ; id }

  def getInitBody(pos : Option[(Int, Int)]) = {
    val defx = 1 //random.nextInt(10)
    val defy = 1 //random.nextInt(10)
    val (x, y) = pos match {
      case Some((myX : Int, myY : Int)) => (myX, myY)
      case None => (defx, defy)
    }
    //val (x, y) = pos.getOrElse((defx, defy)) //TODO why it don't work
    Body("id:" + MapRoom.getNextId, x, y)
  }
 
  def Msg(kind : String, data : JsValue) = JsObject(Seq("type" -> JsString(kind), "data" -> data))
  
}


class MapRoom(myMap : MapSurface) extends Actor {
  case class BodyInter(var b : MapSurface.Body, actor : OutputChannel[Any])

  var members = Map.empty[String, BodyInter]
  
  
  def rootMove(b : MapSurface.Body) {
     members(b.id).b = b
     toAll( Move(b) )
  }
  
  
  def act() = {
    loop {
    receive {
      case IJoin(pos) =>
        val bi = BodyInter(MapRoom.getInitBody(pos), sender)
        sender ! FirstConnectionInfo(bi.b, members.values.toSeq.map(_.b), myMap)
        
        members = members + (bi.b.id -> bi)
        toAll( HasJoined(bi.b) )
        
      case Quit(id) =>
        try {
        	val b = members(id).b
        	members = members - id
        	toAll( SomeoneQuit(b) )
        } catch {
          case _ => println("in quitting, already not here")
        }

      case Move(cli_b) =>
        
        val id = cli_b.id
        val member = members(id)
        val old_b = member.b
        val actor = member.actor
      
      val moveStepValid = old_b.distanceTo(cli_b) == 1
      val isInside = myMap.isInside(cli_b)

      
      if(moveStepValid && isInside)
        myMap.getAt(cli_b) match {
        case Floor() => rootMove(cli_b)
	    case Block() => rootMove(old_b)
	    case FloorLocalJump(b) => rootMove(old_b.moveTo(b))
        case FloorMapJump(mapName, pos) =>
          actor ! GoJoin(MapRoom.maps(mapName), pos)
          members = members - id
          toAll( SomeoneQuit(old_b) ) //can be to the ClientActor to do this, what do you think ?
      } else {
          rootMove(old_b)
      }

      case x : Any => println("MapRoom receive : ", x)
  }}
  }
  
  def toAll(e : Any) = members.foreach { case (_, BodyInter(_, actor)) => actor ! e }

}


//internal messages
case class GoJoin(mr : MapRoom, pos : Option[(Int, Int)])
case class IJoin(pos : Option[(Int, Int)])

case class HasJoined(b : MapSurface.Body)
case class SomeoneQuit(b : MapSurface.Body)


case class JoinInit(pos : Option[(Int, Int)])
case class JoinInitRes(fci : FirstConnectionInfo, channel : Enumerator[JsValue])
case class Move(b : MapSurface.Body)
case class Quit(stringId: String)



case class FirstConnectionInfo(myBody : MapSurface.Body, otherBody : Seq[MapSurface.Body], ms : MapSurface)

object FirstConnectionInfo {
	  def writes(fci : FirstConnectionInfo): JsValue = JsObject(List(
	      "me" -> MapSurface.Body.writes(fci.myBody),
	      "other" -> JsArray(fci.otherBody.map( Body.writes(_) )),
	      "map" -> MapSurface.writes(fci.ms)
	  ))
}


