package models
import models.msg_json._
import actors.Actor
import models.ExternalLink.ExternalLink._
import persistance.map._
import akka.actor.IO.Connected
import persistance.User

import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.global._
import models.persistance.mongoContext._

abstract class PlayerLinkState {
  def act(a : Any) : Any
}



case class PlayerNotConnected(pl : PlayerLink) extends PlayerLinkState {

  def act(a : Any) = a match {
    case FROM_LINK(GetPlayerList()) =>
      pl.wsLink.get ! PlayerList(User.getUserNames())

    case FROM_LINK(PlayerCredential(username, password)) =>

      val user = User.loggingToOne(username, password)
      //TODO check if the user is not already connected
      //To that, use the User as a Actor
      //TODO the loggingToOne should return a msg :
      // - user already connected
      // - user don't exists
      // - bad password

      if(user == None){
        pl.wsLink.get ! KOPlayerCredential("bad stuff here")
      } else {
        pl.wsLink.get ! OKPlayerCredential()
        pl.setState( PlayerConnected(pl, user.get) )
      }

      //TODO player creating
  }

}



case class PlayerConnected(pl : PlayerLink, user : User) extends PlayerLinkState {

  def act(a : Any) = a match {
    case FROM_LINK(Ask_Map()) =>
      val (def_name, def_pos) = MapRoom.defaultMapInfo

      val name = user.mapName.getOrElse(def_name)
      val pos = user.curPos.getOrElse(def_pos)

      val map = MapRoom.getMap(name)
      val curMapInfo = (map !? PlayerJoin(pos, pl)) match { case e:CurrentMap => e }

      pl.wsLink.get ! curMapInfo

      pl.setState( PlayerOnMap(pl, user, curMapInfo.your_body, map) )
  }

}


case class PlayerOnMap(pl : PlayerLink, user : User, body : Body, mapLink : Actor) extends PlayerLinkState {

  def act(a : Any) = a match {
    case e : Any => println("=>>>>>> " + e.toString)
  }

}





class PlayerLink() extends Actor {

  var state : PlayerLinkState = new PlayerNotConnected(this)
  var wsLink : Option[Actor] = None

  def setState(newState : PlayerLinkState) { state = newState }
  def setWs(actor : Actor) { this.wsLink = Some(actor) }

  def act = loop {
    receive {
      case e : Any => state.act(e)
    }
  }

  //var body : Option[Body] = None
  //var mapLink : Option[Actor] = None

  //var servLink : Option[Actor] = None

  //var movingServName : Option[String]  = None
  //var movingFuturePlayer : Option[PlayerJumpingInit] = None

  //def act = {

  //  loop {
  //    receive { case a : Any =>
  //      state match {
  //        case PlayerLinkState.notLinkedYet => act_state_NotLinkedYet(a)
  //        case PlayerLinkState.linkedToMap => act_state_linkedToMap(a)
  //        case PlayerLinkState.changingServer => act_state_changingServer(a)
  //      }
  //    }
  //  }
  //}


  //def act_state_NotLinkedYet(a : Any) = a match {

  //  case FROM_LINK(Ask_Map()) =>
  //    if(movingFuturePlayer != None){
  //      val mfp = movingFuturePlayer.get

  //      mapLink = Some(MapRoom.getMap(mfp.mapName))
  //      mapLink.get ! PlayerJoin(mfp.pos, this)

  //    } else {
  //      assert(mapLink == None)


  //      val (defMapName, defPos) = MapRoom.defaultMapInfo
  //      mapLink = Some(MapRoom.getMap(defMapName))

  //      mapLink.get ! PlayerJoin(defPos, this)
  //    }
  //    state = PlayerLinkState.linkedToMap


  //  case ChangeMap(mapRoom, pos) =>
  //    if(mapLink != None){
  //      mapLink.get ! Quit(this.body.get.id)
  //      wsLink.get ! YouQuit()
  //    }

  //    mapLink = Some(mapRoom)
  //    mapLink.get ! PlayerJoin(pos, this)

  //  case cm : CurrentMap =>
  //    body = Some(cm.your_body)
  //    wsLink.get ! cm

  //  case FROM_LINK(mm : Me_Move) => mapLink.get ! (body.get.id, mm)

  //  case e : CONNECTION_END =>
  //    //wsLink = None
  //    this.mapLink.get ! Quit(this.body.get.id)
  //    exit

  //  //se we wait for the map to know we are no more
  //  //and other element can make us quitting
  //  case Player_Quit(id) =>
  //    if(id != body.get.id) wsLink.get ! Player_Quit(id)

  //  case pm : Player_Move => wsLink.get ! pm
  //  case pj : Player_Join => wsLink.get ! pj
  //  //case q : Quit => wsLink.get ! YouQuit()

  //  case FloorServerJump(servName, mapName, pos) =>
  //    mapLink.get ! Quit(body.get.id)
  //    wsLink.get ! YouQuit()

  //    servLink = Some(models.Servers.getServerLink(servName, this))

  //    servLink.get ! PlayerJumpingInit(mapName, pos)

  //    movingServName = Some(servName)

  //  case FROM_LINK(PlayerJumpingId(id)) => id
  //  val url = Servers.getMoveUrl(movingServName.get, id)
  //  wsLink.get ! YouJump(url)

  //  wsLink.get ! 'quit
  //  exit

  //  case x : Any => println("ClientActor receive : ", x)



  //}

  //
  //
  //def act_state_linkedToMap(a : Any) = a match {
  //  //when unlink, use the actor !?

  //  case FROM_LINK(Ask_Map()) =>
  //    if(movingFuturePlayer != None){
  //      val mfp = movingFuturePlayer.get

  //      mapLink = Some(MapRoom.getMap(mfp.mapName))
  //      mapLink.get ! PlayerJoin(mfp.pos, this)

  //    } else {
  //      assert(mapLink == None)


  //      val (defMapName, defPos) = MapRoom.defaultMapInfo
  //      mapLink = Some(MapRoom.getMap(defMapName))

  //      mapLink.get ! PlayerJoin(defPos, this)
  //    }


  //  case ChangeMap(mapRoom, pos) =>
  //    if(mapLink != None){
  //      mapLink.get ! Quit(this.body.get.id)
  //      wsLink.get ! YouQuit()
  //    }

  //    mapLink = Some(mapRoom)
  //    mapLink.get ! PlayerJoin(pos, this)

  //  case cm : CurrentMap =>
  //    body = Some(cm.your_body)
  //    wsLink.get ! cm

  //  case FROM_LINK(mm : Me_Move) => mapLink.get ! (body.get.id, mm)

  //  case e : CONNECTION_END =>
  //    //wsLink = None
  //    this.mapLink.get ! Quit(this.body.get.id)
  //    exit

  //  //se we wait for the map to know we are no more
  //  //and other element can make us quitting
  //  case Player_Quit(id) =>
  //    if(id != body.get.id) wsLink.get ! Player_Quit(id)

  //  case pm : Player_Move => wsLink.get ! pm
  //  case pj : Player_Join => wsLink.get ! pj
  //  //case q : Quit => wsLink.get ! YouQuit()

  //  case FloorServerJump(servName, mapName, pos) =>
  //    mapLink.get ! Quit(body.get.id)
  //    wsLink.get ! YouQuit()

  //    servLink = Some(models.Servers.getServerLink(servName, this))

  //    servLink.get ! PlayerJumpingInit(mapName, pos)

  //    movingServName = Some(servName)
  //    state = PlayerLinkState.changingServer

  //  case FROM_LINK(PlayerJumpingId(id)) => id
  //  val url = Servers.getMoveUrl(movingServName.get, id)
  //  wsLink.get ! YouJump(url)

  //  wsLink.get ! 'quit
  //  exit

  //  case x : Any => println("ClientActor receive : ", x)


  //}

  //def act_state_changingServer(a : Any) = a match {

  //  case e : CONNECTION_END =>
  //    //wsLink = None
  //    this.mapLink.get ! Quit(this.body.get.id)
  //    exit

  //  //se we wait for the map to know we are no more
  //  //and other element can make us quitting
  //  case Player_Quit(id) =>
  //    if(id != body.get.id) wsLink.get ! Player_Quit(id)

  //  case FROM_LINK(PlayerJumpingId(id)) => id
  //  val url = Servers.getMoveUrl(movingServName.get, id)
  //  wsLink.get ! YouJump(url)

  //  wsLink.get ! 'quit
  //  exit

  //  case x : Any => println("ERROR : ClientActor receive : ", x)
  //}

}


