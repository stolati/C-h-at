import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "C(h)at"
    val appVersion      = "1.0-SNAPSHOT"

    //I don't know in which one I have to put it

    val appDependencies = Seq(
      "org.jfarcand" % "wcs" % "1.2",
      "net.vz.mongodb.jackson" %% "play-mongo-jackson-mapper" % "1.0.0"
    )

    val libraryDependencies = Seq(
      "org.jfarcand" % "wcs" % "1.2",
      "net.vz.mongodb.jackson" %% "play-mongo-jackson-mapper" % "1.0.0"
    )


    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
