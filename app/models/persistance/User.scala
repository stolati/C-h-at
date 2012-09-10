package models.persistance

import map.Pos
import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import mongoContext._
import java.security.MessageDigest
import models.persistance.User.Security

trait AbstractUser {
  def getPos : Option[Pos]
  def getMapName : Option[String]
}

case class User (id: ObjectId = new ObjectId,
                 username: String,
                 var password: String,
                 salt: String = Security.nextSalt,
                 added: Date = new Date(),
                 var mapName: Option[String] = None,
                 var curPos : Option[Pos] = None
                 ) extends AbstractUser {
  override def getPos = curPos
  override def getMapName = mapName
}

object User extends ModelCompanion[User, ObjectId] {
  val collection = mongoCollection("users")
  val dao = new SalatDAO[User, ObjectId](collection = collection) {}

  //TODO double check for potential holes in the security
  object Security {

    val rand = new java.security.SecureRandom
    def nextSalt = {
      import org.apache.commons.codec.binary.Base64

      val aeskey = new Array[Byte](50)
      rand.nextBytes(aeskey)
      new String(Base64.encodeBase64(aeskey))
    }

    def simpleHash(ba : Array[Byte]) = {
      val md5 = MessageDigest.getInstance("SHA")
      md5.update(ba)
      md5.digest
    }

    def getPassHash(password : String, salt : String): String = {
      import java.security.MessageDigest
      import org.apache.commons.codec.binary.Base64

      var bar = (password + salt).getBytes("UTF-8")
      (0 until 1000).foreach(i => bar = simpleHash(bar))

      new String(Base64.encodeBase64(bar))
    }

  }

  def isPassRight(password : String, user : User) = user.password == Security.getPassHash(password, user.salt)

  def loggingToOne(username : String, password : String): Option[User] = {
    val user = findOneByUsername(username)
    if(user == None || !isPassRight(password, user.get)){
      None
    } else {
      user
    }
  }


  def createUser(username : String, password : String) = {
    val searched_user = findOneByUsername(username)
    if (searched_user != None) {
      None
    } else {
      val user = User(username = username, password = "")
      user.password = Security.getPassHash(password, user.salt)
      User.save(user)
      Some(user)
    }
  }

  def findOneByUsername(username: String): Option[User] = dao.findOne(MongoDBObject("username" -> username))

  def getUserNames() = dao.primitiveProjections[String](MongoDBObject(), "username")
}
