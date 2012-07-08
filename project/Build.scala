import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "C(h)at"
    val appVersion      = "1.0-SNAPSHOT"

    //I don't know in which one I have to put it

    val appDependencies = Seq(
      //"org.jfarcand" % "wcs-all" % "1.2",
      "org.jfarcand" % "wcs" % "1.2"
    )

    val libraryDependencies = Seq(
      //"org.jfarcand" % "wcs-all" % "1.2",
      "org.jfarcand" % "wcs" % "1.2"
    )


    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
