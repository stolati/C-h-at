package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.ExternalLink.WebServicePassif
import models.msg_json.MSG_JSON.Id

import com.novus.salat._
import javax.validation.metadata.Scope

object TestCtrl extends Controller{

  def index = Action {
    //Session session = Scope.values()
    //Ok(views.html.echo("test01", session.toString()))
    Ok(views.html.echo("test01","toto"))
  }


}



