import com.google.common.net.InetAddresses
import com.mongodb.casbah.Imports._
import models.persistance.{map, Address, User}
import models.persistance.server.Server
import play.api._
import scala.Some
import se.radley.plugin.salat._
import play.api.Play.current

import models.persistance.conf._
import models.persistance.server._
import models.persistance.map._
import models.persistance._

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    val version = Conf.getInt("C-h-at database version", 0)
    val isInitiated = Conf.getBool("initiated", false)

    println("### launching C-h-at instance ###")
    println("version = " + version)
    println("initiated = " + isInitiated)

    println("#################################")
    println("#################################")
    println("# doing processing begin . . .  #")

    println("# doing processing end   . . .  #")
    println("#################################")
    println("#################################")


    if(!isInitiated){
      createMaps()
      createServer()
      Conf.set("initiated", true)
    }

  }


  def createMaps(){


    val content1="SS000000000000000000JSX000X0XXX0XXXX00000J0XX0XX00X00X00000000J0X0X0X00X00X00000000J0X000X0XXX0XXXX00000J" +
                 "00000000000000000000J00000000000000000000J00000000000000000000J00XX00X00X0XX0000000J0X00X0XX0X0X0X000000J" +
                 "0XXXX0X0XX0X0X000000J0X00X0X00X0XX0000000J0000000000000000XXXXJ0000000000000000000SJ0000000000000000XXXXJ" +
                 "0XX000XX00X00X000000J0X0X0X00X0XX0X000000J0X0X0XXXX0X0XX00X00XJ0XX00X00X0X00X00X00XJ0000000000000000X00XJ" +
                 "JJJJJJJJJJJJJJJJJ22JJ"
    val decypher1=Map("0" -> Floor(),
                      "X" -> Block(),
                      "J" -> FloorLocalJump(pos = map.Pos(6, 6)),
                      "2" -> FloorMapJump(map = "map2", pos = map.Pos(1, 1)),
                      "S" -> FloorServerJump(server = "S1", map = "map4", pos = map.Pos(1, 1))
                  )
    val map1 = MapSurfaceDB(name = "map1", size=Size(21, 21), content=content1, content_decrypt = decypher1)
    println("map1 test consistence : " + map1.checkConsistence())

    val content2="000000000000000000030000000000000000000300000000000000000003"
    val decypher2=Map("0" -> Floor(), "3" -> FloorMapJump(pos = map.Pos(1, 1), map = "map3") )
    val map2 = MapSurfaceDB(name = "map2", size=Size(20, 3), content = content2, content_decrypt = decypher2)
    println("map2 test consistence : " + map2.checkConsistence())

    val content3="000X0X101101101X0X101101101X0X101101101X0X101101101X0X101111"
    val decypher3=Map("0" -> Floor(), "X" -> Block(), "1" -> FloorMapJump(pos = map.Pos(5, 5),map = "map1") )
    val map3 = MapSurfaceDB(name = "map3", size=Size(3, 20), content = content3, content_decrypt = decypher3)
    println("map3 test consistence : " + map3.checkConsistence())

    val content4 = "XX1000000X0000000000000000000XXX0000000XXX00000000XX0000000XX00"
    val decypher4 = Map("0" -> Floor(), "X" -> Block(), "1" -> FloorMapJump(pos = map.Pos(2, 2),map = "map3"))
    val map4 = MapSurfaceDB(name = "map4", size=Size(9, 7), content = content4, content_decrypt = decypher4)
    println("map4 test consistence : " + map4.checkConsistence())


    Conf.set("very_first_map", "map1")
    Conf.set("very_first_map.x", 16.0)
    Conf.set("very_first_map.y", 6.0)

    MapSurfaceDB.save(map1)
    MapSurfaceDB.save(map2)
    MapSurfaceDB.save(map3)
    MapSurfaceDB.save(map4)

  }



  def createServer(){

    val baseUrl = "localhost:9000"
    println("baseUrl : " + baseUrl)

    Server.save(Server(name="S1", url="localhost:9000"))
    Server.save(Server(name="localhost", url="localhost:9000"))

  }



}
