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
import models.ClientLink._


class ClientActor(var curMap : MapRoom, channel : ClientLink) extends Actor {
  case class ClientClosed()
  case class JsFromClient(kind : String, data : JsValue)
  
  var id : String = ""
  
  //link to channel
  channel.setSend { (k : String, d : JsValue) => this ! JsFromClient(k, d) }
  channel.setEnd { () => this ! ClientClosed() }

  def initOn(map : MapRoom, pos : Option[(Int, Int)]) = this ! GoJoin(map, pos)
  
  def act = {
    loop { receive {
      
      case GoJoin(cm, pos) =>
        if(curMap != cm || id != ""){
          
          channel.push("disconnected")
          curMap ! Quit(id)
        }
        
        this.curMap = cm
        this.curMap ! IJoin(pos)
        
      case fc : FirstConnectionInfo =>
          this.id = fc.myBody.id
          channel.push("first_connect", FirstConnectionInfo.writes(fc))

      case HasJoined(b) => channel.push("join", Body.writes(b))
      case SomeoneQuit(b) => channel.push("quit", Body.writes(b))
      case Move(b) => channel.push("setPlayer", Body.writes(b))

      case ClientClosed() =>
        curMap ! Quit(id)

        exit

      case JsFromClient("move", b) => curMap ! Move(Body.reads(b))

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
        val cli = member.actor
      
      val moveStepValid = old_b.distanceTo(cli_b) == 1
      val isInside = myMap.isInside(cli_b)
 
      
      if(moveStepValid && isInside)
        myMap.getAt(cli_b) match {
        case Floor() => rootMove(cli_b)
        case Block() => rootMove(old_b)
        case FloorLocalJump(b) => rootMove(old_b.moveTo(b))
        case FloorMapJump(mapName, pos) =>
          cli ! GoJoin(MapRoom.maps(mapName), pos)
          members = members - id
          toAll( SomeoneQuit(old_b) ) //can be to the ClientActor to do this, what do you think ?

        case FloorServerJump(servName, mapName, pos) =>
          rootMove(old_b)

          actor {
            val servActor = models.Servers.getServerLink(servName)
            val (x, y)  = pos.getOrElse((0, 0))
            servActor.push("newPlayer", Body.writes(Body("", x, y)))




            //TODO stuff here
            println("moving the elemnt to another server")

          }
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


case class ServerPlayerTransfer(b : MapSurface.Body, mapName : String)

object ServerPlayerTransfer {
  def writes(spt : ServerPlayerTransfer): JsValue = JsObject(List(
    "b" -> MapSurface.Body.writes(spt.b),
    "map" -> JsString(spt.mapName)
  ))

  def reads(json: JsValue) = ServerPlayerTransfer(
    MapSurface.Body.reads(json \ ""),
    (json \ "map").as[String]
  )

}


case class ServerPlayerId(id : String)

object ServerPlayerId {
  def writes(spi : ServerPlayerId): JsValue = JsObject(List(
    "id" -> JsString(spi.id)
  ))

  def reads(json: JsValue) = ServerPlayerId(
    (json \ "id").as[String]
  )
}




case class FirstConnectionInfo(myBody : MapSurface.Body, otherBody : Seq[MapSurface.Body], ms : MapSurface)

object FirstConnectionInfo {
	  def writes(fci : FirstConnectionInfo): JsValue = JsObject(List(
	      "me" -> MapSurface.Body.writes(fci.myBody),
	      "other" -> JsArray(fci.otherBody.map( Body.writes(_) )),
	      "map" -> MapSurface.writes(fci.ms)
	  ))
}


