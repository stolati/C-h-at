package models.Mgs

import play.api.libs.{json => play_json}
import com.codahale.jerkson.{Json => jerkson}

//when adding an element,
// - put it in the case class list
// - add it in the fromJson match
// - add it in the toJson match


object JSON_MSG {

  //put here the message that transit throught the network

  case class Person(fn: String, ln: String)





  //helper functions

  def fromJson(s : String) : Any = fromJson(play_json.Json.parse(s))

  def fromJson(json : play_json.JsValue) : Any = {
    val (kind, data) = ( (json \ "kind").as[String], play_json.Json.stringify(json \ "data") )
    kind match {
      case "Person" => jerkson.parse[Person](data)
    }
  }

  def toJsonPlay(o : Any) = play_json.Json.parse(toJson(o))

  def toJson(o : Any) = o match {
    case e : Person => _toJson(e, "Person")

  }

  def _toJson(o : Any, kind : String) = {
    play_json.Json.stringify(play_json.JsObject(Seq(
      "kind" -> play_json.JsString(kind),
      "data" -> play_json.Json.parse(jerkson.generate(o))
    )))
  }

}


