package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.ExternalLink.WebServicePassif


object JS_MSG {


  case class Person(id: Long, name : String)
  implicit object PersonFormat extends Format[Person] {
    def reads(json: JsValue): Person = Person(
      (json \ "id").as[Long],
      (json \ "name").as[String]
    )
    def writes(p : Person): JsValue = JsObject(List(
      "id" -> JsNumber(p.id),
      "name" -> JsString(p.name)
    ))
  }

}




object Canvas extends Controller {

  val defaultMapName = "map1"
  
  def index = Action {
    println("tutu")

    val js = Json.toJson[JS_MSG.Person]( JS_MSG.Person(100343, "toto"))
    println(js)



    Ok(views.html.canvas("hello"))
  }
  
  def ws = WebSocket.async[JsValue]{ request =>

    val ca = new PlayerLink()
    val cl = new WebServicePassif(ca)

    cl.start()
    ca.start()

    cl.getPromise()
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