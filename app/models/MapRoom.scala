package models

import play.api.libs.json._
import scala.util.Random

import actors.{OutputChannel, Actor}

import models.MapSurface._
import models.ExternalLink.ExternalLink._

import models.msg_json.MSG_JSON._

//messages for between PlayerLink and MapSurface
case class Quit(id : Id)
case class ChangeMap(mapRoom : MapRoom, pos : Option[Pos])



class PlayerLink() extends Actor {

  var body : Option[Body] = None
  var mapLink : Option[Actor] = None
  var wsLink : Option[Actor] = None

  def setWs(actor : Actor) { this.wsLink = Some(actor) }

  def act = {
    //TODO try to find a suitable map

    loop {
      receive {

        /*case ChangeMap(cm, pos) =>
          if(curMap != cm || id != ""){

            channel.push("disconnected")
            curMap ! Quit(id)
          }

          this.curMap = cm
          this.curMap ! IJoin(pos)
        */

        case cm : CurrentMap =>
          this.body = Some(cm.your_body)
          this.wsLink.get ! cm

        case FROM_LINK(hj : Player_Join) => this.wsLink.get ! hj
        case FROM_LINK(pq : Player_Quit) => this.wsLink.get ! pq
        case FROM_LINK(pm : Player_Move) => this.wsLink.get ! pm

        case e : CONNECTION_END =>
          this.mapLink.get ! Quit(this.body.get.id)
          exit

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

  def getInitBody(pos : Pos) = {
    val defx = 1 //random.nextInt(10)
    val defy = 1 //random.nextInt(10)
    val myX = if(pos.x == -1) { defx } else { pos.x }
    val myY = if(pos.x == -1) { defy } else { pos.y }

    Body(Id("id:" + MapRoom.getNextId), Id(), Pos(myX, myY))
  }
 
  def Msg(kind : String, data : JsValue) = JsObject(Seq("type" -> JsString(kind), "data" -> data))
  
}


class MapRoom(myMap : MapSurface) extends Actor {
  case class BodyInter(var b : Body, actor : OutputChannel[Any])

  var members = Map.empty[Id, BodyInter]
  
  
  def rootMove(b : Body) {
     members(b.id).b = b
     toAll( Player_Move(b.id, b.pos) )
  }
  
  
  def act() = {
    loop {
    receive {


      case Player_Join(id, pos) =>
        val bi = BodyInter(MapRoom.getInitBody(pos), sender)
        sender ! CurrentMap(bi.b, members.values.toSeq.map(_.b), myMap.toMapSurfaceVisible)
        
        members = members + (bi.b.id -> bi)
        toAll( Player_Join(bi.b.id, bi.b.pos) )
        
      case Quit(id) =>
        try {
        	val b = members(id).b
        	members = members - id
        	toAll( Player_Quit(id) )
        } catch {
          case _ => println("in quitting, already not here")
        }

      case Player_Move(id, new_pos) =>

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
          toAll( Player_Quit(old_b.id) ) //can be to the ClientActor to do this, what do you think ?

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



