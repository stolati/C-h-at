package models

import org.jfarcand.wcs._
import play.api.libs.json._
import actors.Actor
import models.ExternalLink._
import models.msg_json._
import models.ExternalLink.ExternalLink.FROM_LINK
import models.persistance._
import models.persistance.map._


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
}



class ServerLink() extends Actor {

  var wsLink : Option[Actor] = None

  def setWs(a : Actor) = wsLink = Some(a)

  def act = loop {
    receive {
      case FROM_LINK(pji : PlayerJumpingInit) =>
        println(this, "received PlayerJumpingInit")
        val id = TempUser.add(pji.mapName, Pos.fromPosition(pji.pos))
        wsLink.get ! PlayerJumpingId(Id(id))

      case ce : ExternalLink.ExternalLink.CONNECTION_END =>
        println("end of my connection")
        exit

      case e : Any => println("server received something unusual", e)
    }
  }


}

