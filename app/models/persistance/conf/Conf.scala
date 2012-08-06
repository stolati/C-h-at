package models.persistance.conf

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._

//keep it !!! see salat
import models.persistance.mongoContext._

case class Conf (
                 id: ObjectId = new ObjectId,
                 key: String,
                 value_str: Option[String] = None,
                 value_int: Option[Int] = None,
                 value_bool: Option[Boolean] = None,
                 value_double: Option[Double] = None
                 )

object Conf extends ModelCompanion[Conf, ObjectId] {
  val collection = mongoCollection("conf")
  val dao = new SalatDAO[Conf, ObjectId](collection = collection) {}

  def findOneByKey(key : String): Option[Conf] = dao.findOne(MongoDBObject("key" -> key))

  //helpers functions returning Option
  def getStr(key : String): Option[String] = {
    val c = Conf.findOneByKey(key)
    if (c == None) None else c.get.value_str
  }

  def getInt(key : String): Option[Int] = {
    val c = Conf.findOneByKey(key)
    if (c == None) None else c.get.value_int
  }

  def getDouble(key : String): Option[Double] = {
    val c = Conf.findOneByKey(key)
    if (c == None) None else c.get.value_double
  }

  def getBool(key : String): Option[Boolean] = {
    val c = Conf.findOneByKey(key)
    if (c == None) None else c.get.value_bool
  }

  //helpers function with default
  def getStr(key : String, default : String):String = getStr(key).getOrElse(default)
  def getInt(key : String, default : Int):Int = getInt(key).getOrElse(default)
  def getDouble(key : String, default : Double):Double = getDouble(key).getOrElse(default)
  def getBool(key : String, default : Boolean):Boolean = getBool(key).getOrElse(default)

  //setter function
  def set(key : String, value: String) = Conf.save(Conf(key = key, value_str = Some(value)))
  def set(key : String, value: Int) = Conf.save(Conf(key = key, value_int = Some(value)))
  def set(key : String, value: Double) = Conf.save(Conf(key = key, value_double = Some(value)))
  def set(key : String, value: Boolean) = Conf.save(Conf(key = key, value_bool = Some(value)))

}

