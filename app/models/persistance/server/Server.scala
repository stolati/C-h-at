package models.persistance.server

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.ExternalLink._
import models.msg_json.MSG_JSON._
import models.ExternalLink.ExternalLink.FROM_LINK

//keep it !!! see salat
import models.persistance.mongoContext._


case class Server(
                  id : ObjectId = new ObjectId,
                  name : String,
                  url : String
                   ) {

  def getMoveUrl(id : Id) = "http://"  + url + "/canvas?id=" + id.id
  def getWsUrl() = "ws://" + url + "/serv_ws"
}


object Server extends ModelCompanion[Server, ObjectId] {
  val collection = mongoCollection("server")
  val dao = new SalatDAO[Server, ObjectId](collection = collection) {}

  def getByName(name : String): Option[Server] = dao.findOne(MongoDBObject("name" -> name))
}
