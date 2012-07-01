package models

import play.api.libs.json._

sealed abstract class MapElement()
case class Floor() extends MapElement
case class Block() extends MapElement

case class FloorLocalJump(b : MapSurface.Body) extends MapElement
case class FloorMapJump(name : String, pos : Option[(Int, Int)]) extends MapElement


case class MapSurface(h : Int, w : Int, content : Array[Array[MapElement]]) {
  def isInside(b : MapSurface.Body) = ( b.x >= 0 && b.y >= 0 && b.x < w && b.y < h )
  def getAt(b : MapSurface.Body) = content(b.y)(b.x)
}


object MapSurface {
  case class Body(id : String, x : Int, y : Int) {
  def moveTo(newX : Int, newY : Int) = Body(id, newX, newY)
  def moveTo(b : Body) = Body(id, b.x, b.y)
  def distanceTo(b2 : Body) = scala.math.abs(x - b2.x) + scala.math.abs(y - b2.y)
}


object Body {
	def reads(json: JsValue) = Body(
		(json \ "id").as[String],
		(json \ "x").as[Int],
		(json \ "y").as[Int]
	  )
	  def writes(b : Body): JsValue = JsObject(List(
	      "id" -> JsString(b.id),
	      "x" -> JsNumber(b.x),
	      "y" -> JsNumber(b.y)
	  ))
}
  
  
   def writes(ms : MapSurface): JsValue = JsObject(List(
	      "h" -> JsNumber(ms.h),
	      "w" -> JsNumber(ms.w),
	      "content" -> JsArray(ms.content.map( line =>
	        JsArray(line.collect {
	           case Floor() => JsString("F")
	           case Block() => JsString("B")
	           case FloorLocalJump(_) => JsString("F")
	           case FloorMapJump(_, _ ) => JsString("F")
	        })
	      ))
	))
  
  def fromHumain(ground : String, lang : Map[Char, MapElement]) = {
    val content = ground.split('\n').map{_.trim()}.filter(!_.isEmpty()).map{ s =>
      s.map( lang(_) ).toArray
    }
    
    val h = content.size
    val w = (0 /: content.map{_.size}) { scala.math.max(_, _) }
    
    MapSurface(h, w, content)
  }
  
  //represent a mapz
  //0 mean white/traversable
  //1 mean block
  //2 men special => see at the end
  def map1 = fromHumain("""
00000000000000000000J
0X000X0XXX0XXXX00000J
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
00000000000000000000J
00000000000000000000J
00000000000000000000J
0XX000XX00X00X000000J
0X0X0X00X0XX0X000000J
0X0X0XXXX0X0XX00X00XJ
0XX00X00X0X00X00X00XJ
0000000000000000X00XJ
JJJJJJJJJJJJJJJJJ22JJ
  """, Map('0' -> Floor(), 'X' -> Block(), 'J' -> FloorLocalJump(Body("", 6, 6)), '2' -> FloorMapJump("map2", Some(1, 1) )))
  

  def map2 = fromHumain("""
00000000000000000003
00000000000000000003
00000000000000000003
""", Map('0' -> Floor(), 'X' -> Block(), 'J' -> FloorLocalJump(Body("", 1, 1)), '3' -> FloorMapJump("map3", None) ))
  

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
""", Map('0' -> Floor(), 'X' -> Block(), 'J' -> FloorLocalJump(Body("", 1, 1)), '1' -> FloorMapJump("map1", Some(0, 0) )))
  



  def names = Map(
		"map1" -> MapSurface.map1,
		"map2" -> MapSurface.map2,
		"map3" -> MapSurface.map3
  )
  
}




