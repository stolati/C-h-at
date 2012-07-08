package models.msg_json

import play.api.libs.{json => play_json}
import com.codahale.jerkson.{Json => jerkson}

//when adding an element,
// - put it in the case class list
// - add it in the fromJson match
// - add it in the getClassName match


object MSG_JSON {

  //put here the message that transit throught the network

  //general class
  case class Id(id : String = "")
  case class Pos(x : Int, y : Int)
  case class Body(id : Id, map_id : Id, pos : Pos)

  case class MapSurfaceVisible(content : Seq[Seq[MapElementVisible]])
  case class MapElementVisible(kind : String)


  //client -> server class
  case class Me_Move(pos : Pos)
  case class Me_JumpingId(id : Id)

  //server -> client class
  case class Player_Move(id : Id, pos : Pos)
  case class Player_Join(id : Id, pos : Pos)
  case class Player_Quit(id : Id)
  case class CurrentMap(your_body : Body, others_body : Seq[Body], map : MapSurfaceVisible)

  //server -> server
  case class PlayerJumpingInit(body : Body)
  case class PlayerJumpingId(id : Id)





  //helper functions

  def fromJson(s : String) : Any = fromJson(play_json.Json.parse(s))

  def fromJson(json : play_json.JsValue) : Any = {
    val (kind, data) = ( (json \ "kind").as[String], play_json.Json.stringify(json \ "data") )
    kind match {
      case "Id" => jerkson.parse[Id](data)
      case "Pos" => jerkson.parse[Pos](data)
      case "Body" => jerkson.parse[Body](data)
      case "MapSurfaceVisible" => jerkson.parse[MapSurfaceVisible](data)
      case "MapElementVisible" => jerkson.parse[MapElementVisible](data)
      case "Me_Move" => jerkson.parse[Me_Move](data)
      case "Me_JumpingId" => jerkson.parse[Me_JumpingId](data)
      case "Player_Move" => jerkson.parse[Player_Move](data)
      case "Player_Join" => jerkson.parse[Player_Join](data)
      case "Player_Quit" => jerkson.parse[Player_Quit](data)
      case "CurrentMap" => jerkson.parse[CurrentMap](data)
      case "PlayerJumpingInit" => jerkson.parse[PlayerJumpingInit](data)
      case "PlayerJumpingId" => jerkson.parse[PlayerJumpingId](data)
    }
  }

  def toJsonPlay(o : Any) = play_json.Json.parse(toJson(o))

  def toJson(o: Any) = {
    play_json.Json.stringify(play_json.JsObject(Seq(
      "kind" -> play_json.JsString(getClassName(o)),
      "data" -> play_json.Json.parse(jerkson.generate(o))
    )))
  }

  def getClassName(o : Any) = o match {
    case e : Id => "Id"
    case e : Pos => "Pos"
    case e : Body => "Body"
    case e : MapSurfaceVisible => "MapSurfaceVisible"
    case e : MapElementVisible => "MapElementVisible"
    case e : Me_Move => "Me_Move"
    case e : Me_JumpingId => "Me_JumpingId"
    case e : Player_Move => "Player_Move"
    case e : Player_Join => "Player_Join"
    case e : Player_Quit => "Player_Quit"
    case e : CurrentMap => "CurrentMap"
    case e : PlayerJumpingInit => "PlayerJumpingInit"
    case e : PlayerJumpingId => "PlayerJumpingId"
  }

}






















