import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "C(h)at"
    val appVersion      = "1.0-SNAPSHOT"

    //I don't know in which one I have to put it
    val casbah = "com.mongodb.casbah" %% "casbah" % "2.1.5.0"

    val appDependencies = Seq(
      "org.jfarcand" % "wcs" % "1.2",
      //"net.vz.mongodb.jackson" %% "play-mongo-jackson-mapper" % "1.0.0",
      "se.radley" %% "play-plugins-salat" % "1.0.7"
    )

  /*
    val libraryDependencies = Seq(
      "org.jfarcand" % "wcs" % "1.2",
      //"net.vz.mongodb.jackson" %% "play-mongo-jackson-mapper" % "1.0.0",
      "com.mongodb.casbah" %% "casbah" % "2.1.5.0",
      "se.radley" %% "play-plugins-salat" % "1.0.7"
    )
    */


    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      routesImport += "se.radley.plugin.salat.Binders._",
      templatesImport += "org.bson.types.ObjectId"
    )




}
