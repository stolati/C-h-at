package models.persistance.map

import play.api.Play.current
import java.util.Date
import com.novus.salat._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import models.msg_json.MSG_JSON.{MapElementVisible, MapSurfaceVisible}
import models.persistance.map


//keep it !!! see salat
import models.persistance.mongoContext._


/**
 * //@Key("new_name") local_name : type   //rename a key from/to mongo, doing in the case class constructor
 * //@Ignore local_name : type //don't serialize/deserialize it. Should have a default argument
 *
 *
 */


case class Pos(x : Double, y : Double){
  def distance(p: Pos) = math.abs(this.x - p.x) + math.abs(this.y - p.y)
}

case class Size(width : Int, height : Int){
  def tuple = (width, height)
}


@Salat sealed abstract class MapElement()
case class Floor() extends MapElement()
case class Block() extends MapElement()
case class FloorLocalJump(pos : Pos) extends MapElement()
case class FloorMapJump(map : String, pos : Pos) extends MapElement()
case class FloorServerJump(server : String, map : String, pos : Pos) extends MapElement()





/**
 * Represent a map from which a player will move on
 */

case class MapSurfaceDB(
                id: ObjectId = new ObjectId,
                name : String = "",
                size : Size = Size(0, 0), //(x, y) = (width, height)
                content: String = "",
                content_decrypt: Map[Char, MapElement] = Map('c' -> Floor())
                ) {

  def checkConsistence(): Boolean = {
    //test that there is enough element for the given size
    val (width, height) = size.tuple
    if(width * height != content.size) return false
    println("this database size is good")

    //test that each element have a decrypt associated
    if(content.find{ ! content_decrypt.contains(_) } != None) return false
    println("test 2 worked")

    //test that each associated have an element in content
    if(content_decrypt.find{ e => ! content.contains(e._1) } != None) return false
    //TODO see why it don't work like that : ! content.contains(_._1)
    println("test3 worked")

    return true
  }

  def toMapSurface() = {

    //val (width, height) = this.size.tuple;
    val (width, height) = (10, 10)

    val ararelem = (0 until width).map{ x =>
      (0 until height).map{ h =>
        //val pos = (x * height) + width
        //val elem = content_decrypt.get(content(pos)).get
        //elem
        Floor().asInstanceOf[MapElement]
      }.toArray
    }.toArray

    MapSurface(width, height, ararelem)
  }

  //TODO def cleanMe()

}

object MapSurfaceDB extends ModelCompanion[MapSurfaceDB, ObjectId] {
  val collection = mongoCollection("maps")
  val dao = new SalatDAO[MapSurfaceDB, ObjectId](collection = collection) {}

  def findOneByName(name : String) : Option[MapSurfaceDB] = dao.findOne(MongoDBObject("name" -> name))
  def getAll(): List[MapSurfaceDB] = dao.find(MongoDBObject()).toList
}




case class MapSurface(w : Int, h : Int, content : Array[Array[MapElement]]) {

  def isInside(p : Pos) = ( p.x >= 0 && p.y >= 0 && p.x < w && p.y < h )
  def getAt(p : Pos) = content(p.y.toInt)(p.x.toInt)
  def getAllAt(p: Pos, size : (Double, Double)) = {
    var (min_x, max_x) = (math.floor(p.x).toInt, math.ceil(p.x + size._1).toInt)
    var (min_y, max_y) = (math.floor(p.y).toInt, math.ceil(p.y + size._2).toInt)

    (min_x until max_x).map( x =>
      (min_y until max_y).map(y =>
        getAt(Pos(x, y))
      )).flatten
  }


  def toMapSurfaceVisible() = MapSurfaceVisible(
    content.map( line =>
      line.collect {
        case Floor() => "F"
        case Block() => "B"
        case FloorLocalJump(_) => "F"
        case FloorMapJump(_, _ ) => "F"
        case FloorServerJump(_, _, _) => "F"
      }.map(MapElementVisible(_)).toSeq
    ).toSeq
  )
}


//HERE

case object BodyHelper {
  //def moveTo(bfrom : Body, p : Pos) = Body()
  //def moveTo(bfrom : Body, bto : Body) = {}
  //def distanceTo(b1 : Body, b2 : Body) = {}
}


