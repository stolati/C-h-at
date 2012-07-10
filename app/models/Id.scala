package models

import models.msg_json.MSG_JSON.Id
import actors.Actor._

object IdGen {

  lazy val id_actor = {
    val act = actor {
      var curId = 0
      loop {
        react {
          case 'id =>
            curId += 1
            sender ! Id("id_" + curId)
          case 'exit => exit
        }
      }
    }
    act.start()
    act
  }

  def genNext(): Id = (id_actor !? 'id) match { case i : Id => i }
  def stop() { id_actor ! 'exit }
}
