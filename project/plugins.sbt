//name := "C-(h)-at"
//version := "0.0"
//the websocket repository
//val WCS = "org.jfarcand" % "wcs-all" % "1.2"

// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/",
  "OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)
//think about adding thoses repositories :
//https://oss.sonatype.org/content/groups/scala-tools/
//https://oss.sonatype.org/content/repositories/snapshots/

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "2.0.3")

