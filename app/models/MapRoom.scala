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


sealed abstract class MapElement()
case class Floor() extends MapElement
case class Block() extends MapElement

case class FloorLocalJump(goalx : Int, gloaly : Int) extends MapElement



case class MapSurface(h : Int, w : Int, content : Array[Array[MapElement]]) {
  def apply(x : Int, y : Int): MapElement = content(y)(x)
}


object MapSurface {
  
  def fromHumain(ground : String, lang : Map[Char, MapElement]) = {
    val content = ground.split('\n').map{_.trim()}.filter(!_.isEmpty()).map{ s =>
      s.map( lang(_) ).toArray
    }
    
    val h = content.size
    val w = (0 /: content.map{_.size}) { scala.math.max(_, _) }
    
    MapSurface(h, w, content)
  }
  
  
  
  //represent a mapz
  //0 mean white/traversable
  //1 mean block
  //2 men special => see at the end
  def map1 = fromHumain("""
00000000000000000000J
0X000X0XXX0XXXX00000J
0XX0XX00X00X00000000J
0X0X0X00X00X00000000J
0X000X0XXX0XXXX00000J
00000000000000000000J
00000000000000000000J
00000000000000000000J
00XX00X00X0XX0000000J
0X00X0XX0X0X0X000000J
0XXXX0X0XX0X0X000000J
0X00X0X00X0XX0000000J
00000000000000000000J
00000000000000000000J
00000000000000000000J
0XX000XX00X00X000000J
0X0X0X00X0XX0X000000J
0X0X0XXXX0X0XX000000J
0XX00X00X0X00X000000J
00000000000000000000J
JJJJJJJJJJJJJJJJJJJJJ
  """, Map('0' -> Floor(), 'X' -> Block(), 'J' -> FloorLocalJump(6, 6)))
  

 def writes(ms : MapSurface): JsValue = JsObject(List(
	      "h" -> JsNumber(ms.h),
	      "w" -> JsNumber(ms.w),
	      "content" -> JsArray(ms.content.map( line =>
	        JsArray(line.collect {
	           case Floor() => JsString("F")
	           case Block() => JsString("B")
	           case FloorLocalJump(_, _) => JsString("F")
	        })
	      ))
	  ))
  
}





object MapRoom {
  
  lazy val default = {
    val myMap = new MapRoom("toto")
    myMap.start()
    myMap
  }
  
  lazy val random = new Random(12345)

  
  var id = 0
  def getNextId = { id += 1 ; id }

  def getInitBody = Body("id:" + MapRoom.getNextId, random.nextInt(10) + 2, random.nextInt(10) + 2)

  def Msg(kind : String, data : JsValue) = JsObject(Seq("type" -> JsString(kind), "data" -> data))
  
  def receptionWS(event : JsValue, id : String) = { 
      println("reception of : " + event.toString())
      default ! ((event \ "type").as[String] match {
          case "move" => Move(id, Body.reads(event \ "data"))
      })
  }
  
  def join():Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    val speak = (default !? Join())
    
    speak match {
      case (fci : FirstConnectionInfo, channel : Enumerator[JsValue]) => 
        val id = fci.myBody.id
        
        println("connected launched")
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          receptionWS(event, id)
        }.mapDone { _ =>
          default ! Quit(id)
        }
        
        val msg = MapRoom.Msg("first_connect", FirstConnectionInfo.writes(fci))
        val channelWithFirstMsg = Enumerator[JsValue](msg).andThen(channel)
        Akka.future{ (iteratee, channelWithFirstMsg) }
    }

  }
 
}

class MapRoom(mapName : String) extends Actor {
  case class BodyInter(var b : Body, channel : PushEnumerator[JsValue])
  
  var members = Map.empty[String, BodyInter]
  val myMap = MapSurface.map1
  
  def act() = {
    loop {
    receive {
    
    case Join() => {
      println(MapSurface.map1)
      
      val bi = BodyInter(
    	MapRoom.getInitBody,
    	Enumerator.imperative[JsValue]()
      )
      
      val fci = FirstConnectionInfo(bi.b, members.values.toSeq.map( _.b), myMap)
      
      members = members + (bi.b.id -> bi)

      sender ! (fci, bi.channel)
      notifyAll("join", Body.writes(fci.myBody))
    }

    case Move(realId, Body(id, x, y)) => {
      assert(realId == id)
      
      val cli_b = Body(id, x, y)
      val old_b = members(id).b
      
      val moveStep = scala.math.abs(old_b.x - cli_b.x) + scala.math.abs(cli_b.y - old_b.y)
      val moveStepValid = moveStep == 1
      
      val moveOutValid = (cli_b.x >= 0 && cli_b.x < myMap.w) && (cli_b.y >= 0 && cli_b.y < myMap.h)
      
      val new_b = (if(! (moveStepValid && moveOutValid) ) old_b else myMap(x, y) match {
        case Floor() => cli_b
	    case Block() => old_b
	    case FloorLocalJump(goalx, goaly) => Body(id, goalx, goaly)
      })
      
      members(id).b = new_b
      notifyAll("move", Body.writes(new_b))
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
case class Join()
case class Move(id : String, b : Body)
case class Quit(stringId: String)


case class FirstConnectionInfo(myBody : Body, otherBody : Seq[Body], ms : MapSurface)

object FirstConnectionInfo {
	  def writes(fci : FirstConnectionInfo): JsValue = JsObject(List(
	      "me" -> Body.writes(fci.myBody),
	      "other" -> JsArray(fci.otherBody.map( Body.writes(_) )),
	      "map" -> MapSurface.writes(fci.ms)
	  ))
}

case class Body(id : String, x : Int, y : Int)

object Body {
	def reads(json: JsValue) = Body(
		(json \ "id").as[String],
		(json \ "x").as[Int],
		(json \ "y").as[Int]
	  )
	  def writes(b : Body): JsValue = JsObject(List(
	      "id" -> JsString(b.id),
	      "x" -> JsNumber(b.x),
	      "y" -> JsNumber(b.y)
	  ))
}

