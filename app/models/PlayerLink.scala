package models
import models.msg_json._
import actors.Actor
import models.ExternalLink.ExternalLink._
import persistance.map._
import akka.actor.IO.Connected
import persistance._

import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.global._
import models.persistance.mongoContext._

trait PlayerLinkState {
  def act(a : Any) { throw new Exception("PlayerLinkState receive something =>" + a.toString) }
  def inHook() {}
  def outHook() {}
  def connectionEnd(ex : Exception) {}
}



case class PlayerNotConnected(pl : PlayerLink) extends PlayerLinkState {

  override def act(a : Any) = a match {
    case FROM_LINK(GetPlayerList()) =>
      pl.ws ! PlayerList(User.getUserNames())

    case FROM_LINK(PlayerCredential(username, password)) =>

      val user = User.loggingToOne(username, password)
      //TODO check if the user is not already connected
      //To that, use the User as a Actor
      //TODO the loggingToOne should return a msg :
      // - user already connected
      // - user don't exists
      // - bad password

      if(user == None){
        pl.ws ! KOPlayerCredential("bad stuff here")
      } else {
        pl.ws ! OKPlayerCredential()
        pl.setState( PlayerConnected(pl, user.get, true) )
      }

      //TODO player creating
      //TODO when outHook, free the database

    case FROM_LINK(PlayerJumpingId(id)) =>
      println("PlayerNotConnected got PlayerJumpingId")

      val user = TempUser.getUser(id.id)

      if(user == None){
        pl.ws ! KOPlayerCredential("bad ID given")
      } else {
        pl.ws ! OKPlayerCredential()
        pl.setState( PlayerConnected(pl, user.get, true) )
      }
  }

}



case class PlayerConnected(pl : PlayerLink, user : AbstractUser, automap : Boolean = false) extends PlayerLinkState {

  override def inHook(){
    //the first thing we try to do is connect ourselves to the first map
    if(automap) connectDefaultMap()
  }

  override def act(a : Any) = a match {
    case Quit(_) => println("quitting ??")
  }

  def connectDefaultMap(){
    val (def_name, def_pos) = MapRoom.defaultMapInfo

    val name = user.getMapName.getOrElse(def_name)
    val pos = user.getPos.getOrElse(def_pos)

    val map = MapRoom.getMap(name)
    val curMapInfo = (map !? PlayerJoin(pos, pl)) match { case e:CurrentMap => e }

    pl.ws ! curMapInfo

    pl.setState( PlayerOnMap(pl, user, curMapInfo.your_body, map) )
  }

}

case class PlayerOnMap(pl : PlayerLink, user : AbstractUser, body : Body, mapLink : Actor) extends PlayerLinkState {

  override def act(a : Any) = a match {
    case FROM_LINK(mm : Me_Move) => mapLink ! (body.id, mm)
    case Player_Status(id, pos) => pl.ws ! Player_Status(id, pos)
    case Player_Join(id, pos) => pl.ws ! Player_Join(id, pos)
    case Player_Quit(id) =>
      assert(id != body.id)
      pl.ws ! Player_Quit(id)
    case ChangeMap(map, pos) =>
      //quit the current map
      mapLink ! Quit(body.id) //will be done while outHook
      pl.ws ! Player_Quit(body.id)

      //search for the next map
      val curMapInfo = (map !? PlayerJoin(pos, pl)) match { case e:CurrentMap => e }
      pl.ws ! curMapInfo
      pl.setState( PlayerOnMap(pl, user, curMapInfo.your_body, map) )

    case FloorServerJump(servName, mapName, pos) =>
      //quit the current map
      mapLink ! Quit(body.id) //will be done while outHook
      pl.ws ! Player_Quit(body.id)

      //get infos from server
      val servLink = models.Servers.getServerLink(servName, pl)
      pl.setState( ChangingServer(pl, user, servLink, FloorServerJump(servName, mapName, pos), servName) )
  }

  override def connectionEnd(ex : Exception) {
    mapLink ! Quit(body.id)
  }

}


case class ChangingServer(pl : PlayerLink, user : AbstractUser, servLink : Actor, fsj : FloorServerJump, servName : String) extends PlayerLinkState {

  override def inHook(){
    servLink ! PlayerJumpingInit(fsj.map, fsj.pos.toPosition)
  }

  override def act(a : Any) = a match {
    case FROM_LINK(pji : PlayerJumpingId) =>

      val url = Servers.getMoveUrl(servName, pji.id)
      pl.ws ! YouJump(url)

      //the client should kill itself
      pl.setState(PlayerNotConnected(pl))
  }

}





class PlayerLink() extends Actor {

  var state : PlayerLinkState = new PlayerNotConnected(this)
  var wsLink : Option[Actor] = None

  state.inHook()
  def setState(newState : PlayerLinkState) {
    state.outHook()
    state = newState
    state.inHook()
  }

  def setWs(actor : Actor) { this.wsLink = Some(actor) }
  def ws = this.wsLink.get


  def act = loop {
    receive {
      case CONNECTION_END(ex : Exception) =>
        state.connectionEnd(ex)
        exit
      case e : Any => state.act(e)
    }
  }


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


