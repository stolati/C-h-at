package models

import org.jfarcand.wcs._
import play.api.libs.json._
import actors.Actor
import models.ExternalLink._
import models.msg_json.MSG_JSON._
import models.ExternalLink.ExternalLink.FROM_LINK


object Servers {

  //TODO : don't create a connection each time, use a pool. Maybe another time
  def getServerLink(name : String, a : Actor) = {
    val wsa = new WebServiceActif(a, getServUrl(name))
    wsa.start()
    wsa
  }

  def getMoveUrl(name : String, id : Id) = getServerModel(name).getMoveUrl(id)
  def getServUrl(name : String) = getServerModel(name).getWsUrl()

  def getServerModel(name : String) = models.persistance.server.Server.getByName(name).get


  //TODO transform that into an actor
  var futurePlayers = Map[Id, PlayerJumpingInit]()

}



class ServerLink() extends Actor {

  var wsLink : Option[Actor] = None

  def setWs(a : Actor) = wsLink = Some(a)

  def act = loop {
    receive {
      case FROM_LINK(pji : PlayerJumpingInit) =>
        println(this, "received PlayerJumpingInit")
        val id = IdGen.genNext()
        Servers.futurePlayers += id -> pji

        println(this, "sending PlayerJumpingId")
        wsLink.get ! PlayerJumpingId(id)

      case ce : ExternalLink.ExternalLink.CONNECTION_END =>
        println("end of my connection")
        exit

      case e : Any => println("server received something unusual", e)
    }
  }


}

