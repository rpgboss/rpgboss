import sbt._

import Keys._
import AndroidKeys._

object Settings {
  lazy val common = Defaults.defaultSettings ++ Seq (
    version := "0.1",
    scalaVersion := "2.9.2",
    updateLibgdxTask
   )

  lazy val desktop = Settings.common ++ Seq (
    fork in Compile := true
  )

  lazy val android = Settings.common ++
    AndroidProject.androidSettings ++
    AndroidMarketPublish.settings ++ Seq (
      platformName in Android := "android-8",
      keyalias in Android := "change-me",
      mainAssetsPath in Android := file("common/src/main/resources"),
      unmanagedBase <<= baseDirectory( _ /"src/main/libs" ),
      unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "src/main/libs") }
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
    val commonDest = file("common/lib")
    val commonFilter = new ExactFilter("gdx.jar") |
	new ExactFilter("extensions/gdx-freetype.jar") |
	new ExactFilter("extensions/gdx-audio.jar")
    IO.unzip(zipFile, commonDest, commonFilter)

    val desktopDest = file("desktop/lib")
    val desktopFilter = new ExactFilter("gdx-natives.jar") |
    new ExactFilter("gdx-backend-lwjgl.jar") |
    new ExactFilter("gdx-backend-lwjgl-natives.jar") |
    new ExactFilter("gdx-tools.jar") |
    new ExactFilter("extensions/gdx-freetype-natives.jar") |
    new ExactFilter("extensions/gdx-audio-natives.jar")
    IO.unzip(zipFile, desktopDest, desktopFilter)

    val androidDest = file("android/src/main/libs")
    val androidFilter = new ExactFilter("gdx-backend-android.jar") |
    new ExactFilter("armeabi/libgdx.so") |
    new ExactFilter("armeabi/libandroidgl20.so") |
    new ExactFilter("armeabi/libgdx-freetype.so") |
    new ExactFilter("armeabi/libgdx-audio.so") |
    new ExactFilter("armeabi-v7a/libgdx.so") |
    new ExactFilter("armeabi-v7a/libandroidgl20.so") |
    new ExactFilter("armeabi-v7a/libgdx-freetype.so") |
    new ExactFilter("armeabi-v7a/libgdx-audio.so")
    
    commonFilter
    IO.unzip(zipFile, androidDest, androidFilter)

    // Destroy the file.
    zipFile.delete
    s.log.info("Complete")
  }
}

object LibgdxBuild extends Build {
  val common = Project (
    "common",
    file("common"),
    settings = Settings.common
  )

  lazy val desktop = Project (
    "desktop",
    file("desktop"),
    settings = Settings.desktop
  ) dependsOn common

  lazy val android = Project (
    "android",
    file("android"),
    settings = Settings.android
  ) dependsOn common
}
