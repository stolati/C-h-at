package models
import models.msg_json.MSG_JSON._
import actors.Actor
import models.ExternalLink.ExternalLink._
import persistance.map._
import akka.actor.IO.Connected
import persistance.User

abstract class PlayerLinkState {
  def act(a : Any) : Any
}



class PlayerNotConnected(pl : PlayerLink) extends PlayerLinkState {

  def act(a : Any) = a match {
    case FROM_LINK(PlayerCredential(username, password)) =>
      val user = models.persistance.User.loggingToOne(username, password)
      if(user == None){
        pl.wsLink.get ! KOPlayerCredential()
      } else {
        pl.wsLink.get ! OKPlayerCredential()
        pl.setState( new PlayerConnected(pl, user.get)  )
      }
  }

}



class PlayerConnected(pl : PlayerLink, user : User) extends PlayerLinkState {

  def act(a : Any) = a match {

    case FROM_LINK(Ask_Map()) =>

      //TODO can test if the user is on a map before
      val (mapName, defPos) = if(user.mapName != None) (user.mapName.get, user.curPos.get) else MapRoom.defaultMapInfo
      val map = MapRoom.getMap(mapName)

      val curMap = map !? PlayerJoin(defPos, pl)
//      pl.setState( new )
//
//    case cm : CurrentMap => //response to PlayerJoin
//      body = Some(cm.your_body)
//      wsLink.get ! cm
  }

}






class PlayerLink() extends Actor {

  object PlayerLinkState extends Enumeration {
    type PlayerLinkState = Value
    val notLinkedYet, linkedToMap, changingServer = Value;
  }

  var pl_state: PlayerLinkState = new PlayerNotConnected(this) //default state
  def setState(pls : PlayerLinkState) { this.pl_state = pls }

  var state = PlayerLinkState.notLinkedYet


  var body : Option[Body] = None
  var mapLink : Option[Actor] = None
  var wsLink : Option[Actor] = None

  var servLink : Option[Actor] = None

  var movingServName : Option[String]  = None
  var movingFuturePlayer : Option[PlayerJumpingInit] = None

  def act = {

    loop {
      receive { case a : Any =>
        state match {
          case PlayerLinkState.notLinkedYet => act_state_NotLinkedYet(a)
          case PlayerLinkState.linkedToMap => act_state_linkedToMap(a)
          case PlayerLinkState.changingServer => act_state_changingServer(a)
        }
      }
    }
  }

  def setWs(actor : Actor) { this.wsLink = Some(actor) }

  def act_state_NotLinkedYet(a : Any) = a match {

    case FROM_LINK(Ask_Map()) =>
      if(movingFuturePlayer != None){
        val mfp = movingFuturePlayer.get

        mapLink = Some(MapRoom.getMap(mfp.mapName))
        mapLink.get ! PlayerJoin(mfp.pos, this)

      } else {
        assert(mapLink == None)


        val (defMapName, defPos) = MapRoom.defaultMapInfo
        mapLink = Some(MapRoom.getMap(defMapName))

        mapLink.get ! PlayerJoin(defPos, this)
      }
      state = PlayerLinkState.linkedToMap


    case ChangeMap(mapRoom, pos) =>
      if(mapLink != None){
        mapLink.get ! Quit(this.body.get.id)
        wsLink.get ! YouQuit()
      }

      mapLink = Some(mapRoom)
      mapLink.get ! PlayerJoin(pos, this)

    case cm : CurrentMap =>
      body = Some(cm.your_body)
      wsLink.get ! cm

    case FROM_LINK(mm : Me_Move) => mapLink.get ! (body.get.id, mm)

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

  def act_state_linkedToMap(a : Any) = a match {

    case FROM_LINK(Ask_Map()) =>
      if(movingFuturePlayer != None){
        val mfp = movingFuturePlayer.get

        mapLink = Some(MapRoom.getMap(mfp.mapName))
        mapLink.get ! PlayerJoin(mfp.pos, this)

      } else {
        assert(mapLink == None)


        val (defMapName, defPos) = MapRoom.defaultMapInfo
        mapLink = Some(MapRoom.getMap(defMapName))

        mapLink.get ! PlayerJoin(defPos, this)
      }


    case ChangeMap(mapRoom, pos) =>
      if(mapLink != None){
        mapLink.get ! Quit(this.body.get.id)
        wsLink.get ! YouQuit()
      }

      mapLink = Some(mapRoom)
      mapLink.get ! PlayerJoin(pos, this)

    case cm : CurrentMap =>
      body = Some(cm.your_body)
      wsLink.get ! cm

    case FROM_LINK(mm : Me_Move) => mapLink.get ! (body.get.id, mm)

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
      state = PlayerLinkState.changingServer

    case FROM_LINK(PlayerJumpingId(id)) => id
    val url = Servers.getMoveUrl(movingServName.get, id)
    wsLink.get ! YouJump(url)

    wsLink.get ! 'quit
    exit

    case x : Any => println("ClientActor receive : ", x)


  }

  def act_state_changingServer(a : Any) = a match {

    case e : CONNECTION_END =>
      //wsLink = None
      this.mapLink.get ! Quit(this.body.get.id)
      exit

    //se we wait for the map to know we are no more
    //and other element can make us quitting
    case Player_Quit(id) =>
      if(id != body.get.id) wsLink.get ! Player_Quit(id)

    case FROM_LINK(PlayerJumpingId(id)) => id
    val url = Servers.getMoveUrl(movingServName.get, id)
    wsLink.get ! YouJump(url)

    wsLink.get ! 'quit
    exit

    case x : Any => println("ERROR : ClientActor receive : ", x)
  }

}


