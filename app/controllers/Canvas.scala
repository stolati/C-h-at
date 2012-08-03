package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.ExternalLink.WebServicePassif
import models.msg_json.MSG_JSON.Id

import com.novus.salat._

object Canvas extends Controller {

  val defaultMapName = "map1"
  
  def index = Action {
    Ok(views.html.canvas("hello"))
  }
  
  def ws(id : Option[String]) = WebSocket.async[JsValue]{ request =>

    val ca = new PlayerLink()
    val cl = new WebServicePassif(ca)
    ca.setWs(cl)

    if(id != None) ca ! HasIdJump(Id(id.get))

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