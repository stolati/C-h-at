package models

import play.api.libs.json._
import models.msg_json.MSG_JSON._


sealed abstract class MapElement()
case class Floor() extends MapElement
case class Block() extends MapElement

case class FloorLocalJump(p : Pos) extends MapElement
case class FloorMapJump(name : String, pos : Pos) extends MapElement
case class FloorServerJump(serv_name : String, map_name : String, pos : Pos) extends MapElement



class MapSurface(h : Int, w : Int, content : Array[Array[MapElement]]) {
  def isInside(p : Pos) = ( p.x >= 0 && p.y >= 0 && p.x < w && p.y < h )
  def getAt(p : Pos) = content(p.y)(p.x)

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


case object BodyHelper {
  //def moveTo(bfrom : Body, p : Pos) = Body()
  //def moveTo(bfrom : Body, bto : Body) = {}
  //def distanceTo(b1 : Body, b2 : Body) = {}
}



object MapSurface {

  def distance(p1 : Pos, p2 : Pos) = math.abs(p1.x - p2.x) + math.abs(p1.y - p2.y)


  /*
  case class Body(id : String, x : Int, y : Int) {
    def moveTo(newX : Int, newY : Int) = Body(id, newX, newY)
    def moveTo(b : Body) = Body(id, b.x, b.y)
    def distanceTo(b2 : Body) = scala.math.abs(x - b2.x) + scala.math.abs(y - b2.y)
  }
  */


  def fromHumain(ground : String, lang : Map[Char, MapElement]) = {
    val content = ground.split('\n').map{_.trim()}.filter(!_.isEmpty()).map{ s =>
      s.map( lang(_) ).toArray
    }
    
    val h = content.size
    val w = (0 /: content.map{_.size}) { scala.math.max(_, _) }
    
    new MapSurface(h, w, content)
  }
  
  //represent a mapz
  //0 mean white/traversable
  //1 mean block
  //2 men special => see at the end
  def map1 = fromHumain("""
SS000000000000000000J
SX000X0XXX0XXXX00000J
0XX0XX00X00X00000000J
0X0X0X00X00X00000000J
0X000X0XXX0XXXX00000J
00000000000000000000J
00000000000000000000J
00000000000000000000J
00XX00X00X0XX0000000J
0X00X0XX0X0X0X000000J
0XXXX0X0XX0X0X000000J
0X00X0X00X0XX0000000J
0000000000000000XXXXJ
0000000000000000000SJ
0000000000000000XXXXJ
0XX000XX00X00X000000J
0X0X0X00X0XX0X000000J
0X0X0XXXX0X0XX00X00XJ
0XX00X00X0X00X00X00XJ
0000000000000000X00XJ
JJJJJJJJJJJJJJJJJ22JJ
  """, Map(
      '0' -> Floor(),
      'X' -> Block(),
      'J' -> FloorLocalJump(Pos(6, 6)),
      '2' -> FloorMapJump("map2", Pos(1, 1) ),
      'S' -> FloorServerJump("S1", "map4", Pos(1, 1))
      )
   )
  

  def map2 = fromHumain("""
00000000000000000003
00000000000000000003
00000000000000000003
""", Map('0' -> Floor(), 'X' -> Block(), 'J' -> FloorLocalJump(Pos(1, 1)), '3' -> FloorMapJump("map3", Pos(1, 1)) ))
  

  def map3 = fromHumain("""
000
X0X
101
101
101
X0X
101
101
101
X0X
101
101
101
X0X
101
101
101
X0X
101
111
""", Map('0' -> Floor(), 'X' -> Block(), 'J' -> FloorLocalJump(Pos(1, 1)), '1' -> FloorMapJump("map1", Pos(1, 1) )))


  def map4 = fromHumain("""
XX00000
X0X0000
0XXX000
00XXX00
000XXX0
00000XX
""", Map('0' -> Floor(), 'X' -> Block()))

  def names = Map(
		"map1" -> MapSurface.map1,
		"map2" -> MapSurface.map2,
		"map3" -> MapSurface.map3,
    "map4" -> MapSurface.map4
  )
  
}





















