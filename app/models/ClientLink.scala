package models.ClientLink

//TODO : a big one, change the ClientLink by Actors, they are more on that
//So we can do case transformation :
// elem.toJson() => transform into json
// Elem.fromJson(elem) => get from json => this won't work because elem won't have the elemnt
// search on the Json stuff to see it



import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._
import play.api.Play.current

import scala.util.Random
import java.util.ArrayList

import scala.actors._
import scala.actors.Actor._

import models.MapSurface._

abstract trait ClientLink {
  case class CONNECTION_END()
  
  def push(kind : String, data : JsValue = JsNull)
  def setSend(fct : (String, JsValue) => Unit) //TODO remove Null and found an Unit that is not necessary
  def setEnd(fct : () => Unit) //TODO see above
  
  def push2js(k : String, d : JsValue) = JsObject(Seq("type" -> JsString(k), "data" -> d))
  def js2send(event : JsValue) = ((event \ "type").as[String], event \ "data")
}



class WSClientLink extends ClientLink {
  
  var sendFct : (String, JsValue) => Unit = (k : String, d : JsValue) => {}
  var endFct : () => Unit = () => {}
  
  val enumChannel = Enumerator.imperative[JsValue]()  
  val iteChannel = Iteratee.foreach[JsValue]{ event =>
    println("from client, get : ", event)
    val (kind, data) = js2send(event)
    sendFct(kind, data)
  }.mapDone { _ =>
    println("from client, end of all")
    endFct() }
  
  override def push(kind : String, data : JsValue = JsNull) = enumChannel.push(push2js(kind, data))
  override def setSend(f : (String, JsValue) => Unit) = {sendFct = f}
  override def setEnd(f : () => Unit) = {endFct = f}
  
  def getPromise(): Promise[(Iteratee[JsValue,_],Enumerator[JsValue])] = Akka.future{ (iteChannel, enumChannel) }
}


