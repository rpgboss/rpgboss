import sbt._

import Keys._

object Settings {
  lazy val rpglib = Defaults.defaultSettings ++ Seq (
    version := "0.1",
    scalaVersion := "2.9.2",
    updateLibgdxTask,
    libraryDependencies ++= Seq(
      "postgresql" % "postgresql" % "9.0-801.jdbc4",
      "org.mindrot" % "jbcrypt" % "0.3m",
      "com.google.guava" % "guava" % "10.0",
      "net.liftweb" % "lift-json_2.9.1" % "2.4",
      "com.weiglewilczek.slf4s" % "slf4s_2.9.1" % "1.0.7",
      "org.scalatest" %% "scalatest" % "1.6.1" % "test"
    ),
    scalacOptions += "-deprecation"
   )

  val updateLibgdx = TaskKey[Unit]("update-gdx", "Updates libgdx")

  val updateLibgdxTask = updateLibgdx <<= streams map { (s: TaskStreams) =>
    import Process._
    import java.io._
    import java.net.URL
    
    // Declare names
    val baseUrl = "http://libgdx.badlogicgames.com/nightlies"
    val gdxName = "libgdx-nightly-latest"

    // Fetch the file.
    s.log.info("Pulling %s" format(gdxName))
    s.log.warn("This may take a few minutes...")
    val zipName = "%s.zip" format(gdxName)
    val zipFile = new java.io.File(zipName)
    val url = new URL("%s/%s" format(baseUrl, zipName))
    IO.download(url, zipFile)

    // Extract jars into their respective lib folders.
    val commonDest = file("lib")
    val commonFilter = new ExactFilter("gdx.jar") |
	new ExactFilter("extensions/gdx-freetype.jar") |
	new ExactFilter("extensions/gdx-audio.jar")
    IO.unzip(zipFile, commonDest, commonFilter)

    // Destroy the file.
    zipFile.delete
    s.log.info("Complete")
  }
}

object LibgdxBuild extends Build {
  val rpglib = Project (
    "rpglib",
    file("."),
    settings = Settings.rpglib
  )
}
