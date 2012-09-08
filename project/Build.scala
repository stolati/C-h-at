import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "C(h)at"
    val appVersion      = "1.0-SNAPSHOT"


    val appDependencies = Seq(
      //"org.jfarcand" % "wcs" % "1.2", //websocket lib for scala
      "se.radley" %% "play-plugins-salat" % "1.0.7", //mongoDB serialisation
      //"net.lif-tw-eb" %% "l-ift-js-on" % "2.5", //json serializer-deserializer
      //apache commons
      "commons-io" % "commons-io" % "2.3",
      "commons-codec" % "commons-codec" % "1.6",
      //class utils
      "org.clapper" %% "classutil" % "0.4.6" //class utils
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      routesImport += "se.radley.plugin.salat.Binders._",
      templatesImport += "org.bson.types.ObjectId"
    )


}

//libraryDependencies += groupID % artifactID % revision
//libraryDependencies += groupID %% artifactID % revision //append scala version to the artifactID
//libraryDependencies += groupID % artifactID % revision % configuration

