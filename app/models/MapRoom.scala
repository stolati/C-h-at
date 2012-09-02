package models

import persistance.map._
import play.api.libs.json._
import actors.{OutputChannel, Actor}
import models.ExternalLink.ExternalLink._


import models.msg_json._

//messages for between PlayerLink and MapSurface
case class PlayerJoin(pos : Pos, a : Actor)
case class Quit(id : Id)
case class ChangeMap(mapRoom : MapRoom, pos : Pos)
case class HasIdJump(id : Id)


case class SimpleElement(s : String = "toto")

object MapRoom {

  //TODO have a pool for MapRoom elements
  val liveMaps = new scala.collection.mutable.HashMap[String, MapRoom] with scala.collection.mutable.SynchronizedMap[String, MapRoom]

  def getMap(name : String) : MapRoom = {
    if(!liveMaps.contains(name)){
      println("initisizing another map : " + name)
      val myMap = new MapRoom(MapSurfaceDB.findOneByName(name).get.toMapSurface)
      myMap.start()
      liveMaps += name -> myMap
    }

    liveMaps(name)
  }

  lazy val defaultMapInfo =
    (persistance.conf.Conf.getStr("very_first_map").get,
      Pos(
        persistance.conf.Conf.getDouble("very_first_map.x").get,
        persistance.conf.Conf.getDouble("very_first_map.y").get
      )
    )


  def getInitBody(pos : Pos) = Body(models.IdGen.genNext(), Id(), pos.toPosition)
}


class MapRoom(myMap : MapSurface) extends Actor {
  case class BodyInter(var b : Body, actor : OutputChannel[Any])

  var members = Map.empty[Id, BodyInter]
  
  
  def playerMove(id : Id, pos : Pos) {
     val old_body = members(id).b
     members(id).b = Body(id, old_body.map_id, pos.toPosition)
     this.toAll( Player_Move(id, pos.toPosition) )
  }


  def gotMove(id : Id, new_pos : Pos) {
    println(members)
    val member = members(id)
    val old_pos = member.b.pos

    val moveStepValid = Pos.fromPosition(old_pos).distance(new_pos) <= 1
    val isInside = myMap.isInside(new_pos)

    if(!moveStepValid || !isInside) {
      playerMove(id, Pos.fromPosition(old_pos))
      return
    }

    //find out which is the main element under the feet
    var jumpAction : MapElement = Floor()
    var hasBlock = false

    //priority for block
    myMap.getAllAt(new_pos, size = (1, 1)).collect {
      case Block() => hasBlock = true
      case Floor() =>
      case e : FloorLocalJump  => jumpAction = e
      case e : FloorMapJump    => jumpAction = e
      case e : FloorServerJump => jumpAction = e
    }

    if(hasBlock) {
      playerMove(id, Pos.fromPosition(old_pos) )
      return
    }

    jumpAction match {
      case FloorLocalJump(p) => playerMove(id, p)

      case FloorMapJump(mapName, pos) =>
        member.actor ! ChangeMap(MapRoom.getMap(mapName), pos)

      case FloorServerJump(servName, mapName, pos) =>
        print("server jump stuff")
        member.actor ! FloorServerJump(servName, mapName, pos)
      case Floor() => playerMove(id, new_pos)
      case Block() => assert(false)
    }

  }

  
  
  def act() = loop {
    receive {

      case PlayerJoin(pos, actor_sender) =>
        val bi = BodyInter(MapRoom.getInitBody(pos), actor_sender)
        sender ! CurrentMap(bi.b, members.values.toSeq.map(_.b), myMap.toMapSurfaceVisible)
        members = members + (bi.b.id -> bi)
        toAll( Player_Join(bi.b.id, bi.b.pos) )

      case Quit(id) =>
        try {
          val m = members(id)
          members = members - id

          //m.actor ! Quit(id)
          toAll(Player_Quit(id))
        } catch {
          case _ => println("in quitting, already not here")
        }

      case (id : Id, Me_Move(new_pos : Pos)) => gotMove(id, new_pos)

      case x : Any => println("MapRoom receive : ", x)
    }
  }
  
  def toAll(e : Any) = members.foreach { case (_, BodyInter(_, actor)) => actor ! e }

}



