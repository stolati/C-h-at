import com.mongodb.casbah.Imports._
import play.api._
import se.radley.plugin.salat._

import models.persistance.{User, Address}
import models.persistance.conf.Conf

object Global extends GlobalSettings {

  override def onStart(app: Application) {

    val version = Conf.getInt("C-h-at database version", 0)
    val isInitiated = Conf.getBool("initiated", false)

    println("### launching C-h-at instance ###")
    println("version = " + version)
    println("initiated = " + isInitiated)


    if(!isInitiated){
      databaseFill()
      Conf.set("initiated", true)
    }

  }


  def databaseFill(){

    /****
     * TODO : Doing Database init, depending of version
     * this should use models stuffs, doing file search of database modification
     */

    Logger.info("Loading Testdata")

    User.save(User(
      username = "leon",
      password = "1234",
      address = Some(Address("Örebro", "123 45", "Sweden"))
    ))

    User.save(User(
      username = "guillaume",
      password = "1234",
      address = Some(Address("Paris", "75000", "France"))
    ))

  }



}
