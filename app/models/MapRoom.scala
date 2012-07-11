package models

import play.api.libs.json._
import actors.{OutputChannel, Actor}
import models.MapSurface._
import models.ExternalLink.ExternalLink._

import models.msg_json.MSG_JSON._

//messages for between PlayerLink and MapSurface
case class PlayerJoin(pos : Option[Pos])
case class Quit(id : Id)
case class ChangeMap(mapRoom : MapRoom, pos : Option[Pos])
case class HasIdJump(id : Id)


class PlayerLink() extends Actor {

  var body : Option[Body] = None
  var mapLink : Option[Actor] = None
  var wsLink : Option[Actor] = None

  var servLink : Option[Actor] = None

  var movingServName : Option[String]  = None
  var movingFuturePlayer : Option[PlayerJumpingInit] = None


  def setWs(actor : Actor) { this.wsLink = Some(actor) }

  def act = {

    loop {
      receive {

        case HasIdJump(id)  =>
          val fp = Servers.futurePlayers(id)
          Servers.futurePlayers -= id
          movingFuturePlayer = Some(fp)

        case FROM_LINK(Ask_Map()) =>
          if(movingFuturePlayer != None){
            val mfp = movingFuturePlayer.get

            mapLink = Some(MapRoom.maps(mfp.mapName))
            mapLink.get ! PlayerJoin(Some(mfp.pos))

          } else {
            assert(mapLink == None)

            mapLink = Some(MapRoom.default_map)
            mapLink.get ! PlayerJoin(None)
          }

        case ChangeMap(mapRoom, pos_opt) =>
          if(mapLink != None){
            mapLink.get ! Quit(this.body.get.id)
            wsLink.get ! YouQuit()
          }

          mapLink = Some(mapRoom)
          mapLink.get ! PlayerJoin(pos_opt)

        case cm : CurrentMap =>
          body = Some(cm.your_body)
          wsLink.get ! cm

        case FROM_LINK(mm : Me_Move) => mapLink.get ! (body.get.id, mm)

        //case FROM_LINK(hj : Player_Join) => wsLink.get ! (body.get.id, hj)
        //case FROM_LINK(pq : Player_Quit) => wsLink.get ! (body.get.id, pq)
        //case FROM_LINK(pm : Player_Move) => wsLink.get ! (body.get.id, pm)

        case e : CONNECTION_END =>
          //wsLink = None
          this.mapLink.get ! Quit(this.body.get.id)
          exit

        //se we wait for the map to know we are no more
        //and other element can make us quitting
        case Player_Quit(id) =>
          if(id != body.get.id) wsLink.get ! Player_Quit(id)

        case pm : Player_Move => wsLink.get ! pm
        case pj : Player_Join => wsLink.get ! pj
        //case q : Quit => wsLink.get ! YouQuit()

        case FloorServerJump(servName, mapName, pos) =>
          mapLink.get ! Quit(body.get.id)
          wsLink.get ! YouQuit()

          servLink = Some(models.Servers.getServerLink(servName, this))

          servLink.get ! PlayerJumpingInit(mapName, pos)

          movingServName = Some(servName)

        case FROM_LINK(PlayerJumpingId(id)) => id
          val url = Servers.getMoveUrl(movingServName.get, id)
          wsLink.get ! YouJump(url)

          wsLink.get ! 'quit
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
  
  lazy val default_map = maps("map1")
  
  def getInitBody(pos : Option[Pos]) = Body(models.IdGen.genNext(), Id(), pos.getOrElse(Pos(16, 6)))
  def Msg(kind : String, data : JsValue) = JsObject(Seq("type" -> JsString(kind), "data" -> data))
  
}


class MapRoom(myMap : MapSurface) extends Actor {
  case class BodyInter(var b : Body, actor : OutputChannel[Any])

  var members = Map.empty[Id, BodyInter]
  
  
  def playerMove(id : Id, pos : Pos) {
     val old_body = members(id).b
     members(id).b = Body(id, old_body.map_id, pos)
     this.toAll( Player_Move(id, pos) )
  }


  def gotMove(id : Id, new_pos : Pos) {
    val member = members(id)
    val old_pos = member.b.pos

    val moveStepValid = MapSurface.distance(new_pos, old_pos) <= 1
    val isInside = myMap.isInside(new_pos)

    if(!moveStepValid || !isInside) {
      playerMove(id, old_pos)
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
      playerMove(id, old_pos)
      return
    }

    jumpAction match {
      case FloorLocalJump(p) => playerMove(id, p)

      case FloorMapJump(mapName, pos) =>
        member.actor ! ChangeMap(MapRoom.maps(mapName), Some(pos))

      case FloorServerJump(servName, mapName, pos) =>
        print("server jump stuff")
        member.actor ! FloorServerJump(servName, mapName, pos)
      case Floor() => playerMove(id, new_pos)
      case Block() => assert(false)
    }

  }

  
  
  def act() = loop {
    receive {

      case PlayerJoin(pos) =>
        val bi = BodyInter(MapRoom.getInitBody(pos), sender)
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



