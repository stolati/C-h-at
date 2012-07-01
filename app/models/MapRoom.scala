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
    println("getInitBody%s", pos)
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
  
  def receptionWS(event : JsValue, id : String, curMap : MapRoom) = { 
      println("reception of : " + event.toString())
      curMap ! ((event \ "type").as[String] match {
          case "move" => Move(id, Body.reads(event \ "data"))
      })
  }
  
  def join(name : String):Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    val curMap = maps(name)
    val speak = (curMap !? Join(None))
    
    speak match {
      case (fci : FirstConnectionInfo, channel : Enumerator[JsValue]) => 
        val id = fci.myBody.id
        
        println("connected launched")
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          receptionWS(event, id, curMap)
        }.mapDone { _ =>
          curMap ! Quit(id)
        }
        
        val msg = MapRoom.Msg("first_connect", FirstConnectionInfo.writes(fci))
        val channelWithFirstMsg = Enumerator[JsValue](msg).andThen(channel)
        Akka.future{ (iteratee, channelWithFirstMsg) }
    }

  }
 
}


class MapRoom(myMap : MapSurface) extends Actor {
  case class BodyInter(var b : MapSurface.Body, channel : PushEnumerator[JsValue])
  case class JoinFromMap(bi : BodyInter, pos : Option[(Int, Int)])
  
  var members = Map.empty[String, BodyInter]
  
  def rootMove(b : MapSurface.Body) {
     members(b.id).b = b
     notifyAll("move", Body.writes(b))
  }
  
  
  def act() = {
    loop {
    receive {
      
      
      case JoinFromMap(BodyInter(b, channel), pos) => {
                  println("begin of JoinFromMap")
      
      val bi = BodyInter(
    	MapRoom.getInitBody(pos),
    	channel
      )
      
      val fci = FirstConnectionInfo(bi.b, members.values.toSeq.map( _.b), myMap)
      
      members = members + (bi.b.id -> bi)
      
      println("members added")

      val msg = MapRoom.Msg("first_connect", FirstConnectionInfo.writes(fci))
      println("msg = %s, ", msg)
      channel.push(msg) 
      
      notifyAll("join", Body.writes(fci.myBody))
    }
    
    case Join(pos) => {
      
      println(MapSurface.map1)
      
      val bi = BodyInter(
    	MapRoom.getInitBody(pos),
    	Enumerator.imperative[JsValue]()
      )
      
      val fci = FirstConnectionInfo(bi.b, members.values.toSeq.map( _.b), myMap)
      
      members = members + (bi.b.id -> bi)

      sender ! (fci, bi.channel)
      notifyAll("join", Body.writes(fci.myBody))
    }
    
    
    
    case Move(id, cli_b) => {
      assert(id == cli_b.id)
      
      val member = members(id)
      val old_b = member.b
      val channel = member.channel
      
      println("###############################")
      printf("trying to move to %s\n", cli_b)
      val moveStepValid = old_b.distanceTo(cli_b) == 1
      printf("moveStep = %s\n", old_b.distanceTo(cli_b))
      printf("moveStepValid = %s\n", moveStepValid)
      val isInside = myMap.isInside(cli_b)
      printf("isInside = %s\n", isInside)
      println("###############################")

      
      if(moveStepValid && isInside)
        myMap.getAt(cli_b) match {
        case Floor() => rootMove(cli_b)
	    case Block() => rootMove(old_b)
	    case FloorLocalJump(b) => rootMove(old_b.moveTo(b))
        case FloorMapJump(mapName, pos) =>
          val newMap = MapRoom.maps(mapName)
          
          member.channel.push(MapRoom.Msg("disconnected", JsNull)) 
          notifyAll("quit", Body.writes(old_b))
          
          members = members - id
          println("launching JoinFromMap")
          newMap ! JoinFromMap(member, pos)
          
          //rootMove(old_b)
      } else {
          rootMove(old_b)
      }
    }

    case Quit(id) => {
      val b = members(id).b
      members = members - id
      notifyAll("quit", Body.writes(b))
    }
  }}
  }
  
  def notifyAll(kind: String, data : JsValue) {
    println( Seq("notifyAll : ", kind , " => ", data).mkString("") )
    
    members.foreach { 
      case (_, BodyInter(_, channel)) => channel.push(MapRoom.Msg(kind, data))
    }
  }
  
}


//internal messages
case class Join(pos : Option[(Int, Int)])
case class Move(id : String, b : MapSurface.Body)
case class Quit(stringId: String)



case class FirstConnectionInfo(myBody : MapSurface.Body, otherBody : Seq[MapSurface.Body], ms : MapSurface)

object FirstConnectionInfo {
	  def writes(fci : FirstConnectionInfo): JsValue = JsObject(List(
	      "me" -> MapSurface.Body.writes(fci.myBody),
	      "other" -> JsArray(fci.otherBody.map( Body.writes(_) )),
	      "map" -> MapSurface.writes(fci.ms)
	  ))
}


