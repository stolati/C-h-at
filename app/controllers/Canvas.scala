package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import models._

object CanvasPlaces {
  var allCP = List[CanvasPlaces]()
}


class CanvasPlaces {
  var x : Int = 0
  var y : Int = 0
  //var out : () =>
  
  def connect(){
    CanvasPlaces.allCP = this :: CanvasPlaces.allCP
    println("canvas connected")
    println(CanvasPlaces.allCP)
  }
  
  def unconnect(){
    println("before remove :")
    println(CanvasPlaces.allCP)
    CanvasPlaces.allCP = CanvasPlaces.allCP.filterNot( _ == this )
    println("after remove :")
    
    println(CanvasPlaces.allCP)

  }
  
  def in(msg : JsValue) {
    println("have x : " + (msg \ "x").as[Int] )
    println("have y : " + (msg \ "y").as[Int] )
    println("CanvasPlaces : " + msg)
  }
  
  def end(){
	 println("Canvas Disconnected")
	 this.unconnect()
  }
  
}



object Canvas extends Controller {

  def index = Action {
    Ok(views.html.canvas("hello"))
  }
  
  def ws = WebSocket.async[JsValue]{ request =>
    
    MapRoom.join("map1")
  
    /*
    CanvasPlace.hello()
    
    val cp = new CanvasPlaces()
    cp.connect()
    
  	//log events to the console
  	val in = Iteratee.foreach[JsValue](cp.in).mapDone { _ => cp.end() }
  	//val in = Iteratee.consume[String]() //juste consume and ignore the input
  	
  	val out = Enumerator[JsValue](JsNull)
  	//val out = Enumerator("Hello!") >>> Enumerator.eof //send the message and close
  	
  	(in, out)
  	*/
  }
  
  
}



//building an action : http://localhost:9000/@documentation/ScalaActions
//contain implicit, json (or other body parser)

/*
def index = Action {
  SimpleResult(
    header = ResponseHeader(200, Map(CONTENT_TYPE -> "text/plain")), 
    body = Enumerator("Hello world!")
  )
}
*/

/*
val ok = Ok("Hello world!")
val notFound = NotFound
val pageNotFound = NotFound(<h1>Page not found</h1>)
val badRequest = BadRequest(views.html.form(formWithErrors))
val oops = InternalServerError("Oops")
val anyStatus = Status(488)("Strange response type")
*/

//  Redirect("/user/home")
//   Redirect("/user/home", status = MOVED_PERMANENTLY)