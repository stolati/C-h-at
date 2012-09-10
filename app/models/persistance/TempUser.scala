package models.persistance

import map.Pos
import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
//import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import mongoContext._
import java.security.MessageDigest
import models.persistance.User.Security
import org.bson.types.ObjectId


case class TempUser(id: ObjectId = new ObjectId,
                    added: Date = new Date(),
                    mapName: String,
                    curPos : Pos
                    ) extends AbstractUser {
  override def getPos = Some(curPos)
  override def getMapName = Some(mapName)
}

object TempUser extends ModelCompanion[TempUser, ObjectId] {
  val collection = mongoCollection("temp_users")
  val dao = new SalatDAO[TempUser, ObjectId](collection = collection) {}

  def add(mapName : String, pos : Pos) = {
    val tu = TempUser(new ObjectId, new Date(), mapName, pos)
    TempUser.save(tu)
    object_id.Object_id.objectId2string(tu.id)
  }

  def getUser(s : String) : Option[TempUser] = {
    val id = object_id.Object_id.string2objectId(s)
    dao.findOneById(id)
  }

  //case FROM_LINK(pji : PlayerJumpingInit) =>
  //println(this, "received PlayerJumpingInit")
  //val id = IdGen.genNext()
  //Servers.futurePlayers += id -> pji

  //def isPassRight(password : String, user : User) = user.password == Security.getPassHash(password, user.salt)

  //def loggingToOne(username : String, password : String): Option[User] = {
  //  val user = findOneByUsername(username)
  //  if(user == None || !isPassRight(password, user.get)){
  //    None
  //  } else {
  //    user
  //  }
  //}


  //def createUser(username : String, password : String) = {
  //  val searched_user = findOneByUsername(username)
  //  if (searched_user != None) {
  //    None
  //  } else {
  //    val user = User(username = username, password = "")
  //    user.password = Security.getPassHash(password, user.salt)
  //    User.save(user)
  //    Some(user)
  //  }
  //}

  //def findOneByUsername(username: String): Option[User] = dao.findOne(MongoDBObject("username" -> username))

  //def getUserNames() = dao.primitiveProjections[String](MongoDBObject(), "username")
}
