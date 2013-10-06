import sbt._

import Keys._
import AndroidKeys._
import WebStartPlugin._
import scala.sys.process.{Process => SysProcess}

object Settings {
  lazy val common = Defaults.defaultSettings ++ Seq (
    fork := true, // For natives loading.
    version := "0.1",
    scalaVersion := "2.10.1",
    scalacOptions ++= List("-deprecation", "-unchecked"),
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "10.0",
      "com.typesafe" % "scalalogging-slf4j_2.10" % "1.0.1",
      "net.sf.opencsv" % "opencsv" % "2.0",
      "org.json4s" % "json4s-ast_2.10" % "3.2.5" withSources(),
      "org.json4s" % "json4s-core_2.10" % "3.2.5" withSources(),
      "org.json4s" % "json4s-native_2.10" % "3.2.5" withSources(),
      "org.scalatest" % "scalatest_2.10" % "2.0.RC1" % "test",
      "rhino" % "js" % "1.7R2"
    ),
    unmanagedJars in Compile <<= baseDirectory map { base =>
      var baseDirectories = (base / "lib") +++ (base / "lib" / "extensions")
      var jars = baseDirectories ** "*.jar"
      jars.classpath
    },
    updateLibsTask,
    updateLibgdxTask,
    TaskKey[Unit]("generateEnum") := {  
      SysProcess("python GenerateFileEnum.py", new File("common/src/main/resources")).run()
      println("Generated file enumeration")
      Unit
    },
    Keys.`compile` <<= (Keys.`compile` in Compile) dependsOn TaskKey[Unit]("generateEnum")
   )

  lazy val playerDesktop = Settings.common ++ Seq (
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.0.6"
    ),
    unmanagedJars in Compile <<= baseDirectory map { base =>
      var baseDirectories = (base / "lib") +++ (base / "lib" / "extensions")
      var jars = baseDirectories ** "*.jar"
      jars.classpath
    }
  )

  lazy val playerAndroid = Settings.common ++
    AndroidProject.androidSettings ++
    AndroidMarketPublish.settings ++ Seq (
      platformName in Android := "android-8",
      keyalias in Android := "change-me",
      mainAssetsPath in Android := file("common/src/main/resources"),
      unmanagedJars in Compile <++= baseDirectory map { base =>
        var baseDirectories = 
          (base / "src" / "main" / "libs") +++ 
          (base / "src" / "main" / "libs" / "extensions")
        var jars = baseDirectories ** "*.jar"
        jars.classpath
      }
    )
    
  lazy val editor = Settings.playerDesktop ++ editorLibs ++ editorWebstart
  
  lazy val editorLibs = Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-swing" % "2.10.1",
      "com.github.benhutchison" % "scalaswingcontrib" % "1.5", 
      "org.apache.httpcomponents" % "httpclient" % "4.1.1",
      "net.java.dev.designgridlayout" % "designgridlayout" % "1.8"
    ),
    mainClass in (Compile, run) := Some("rpgboss.editor.RpgDesktop"),
    scalacOptions ++= List("-deprecation", "-unchecked"))
  
  lazy val editorWebstart = webstartSettings ++ Seq(
    webstartGenConfig := GenConfig(
        dname       = "CN=Snake Oil, OU=An Anonymous Hacker, O=Bad Guys Inc., L=Bielefeld, ST=33641, C=DE",
        validity    = 365
    ),
    webstartKeyConfig := KeyConfig(
        keyStore    = file("key/keyStore"),
        storePass   = "password",
        alias       = "alias",
        keyPass     = "password"
    ),
    webstartJnlpConfigs := Seq(JnlpConfig(
        fileName    = "rpgboss.jnlp",
        descriptor  = (fileName:String, assets:Seq[JnlpAsset]) => {
            <jnlp spec="1.5+" codebase="http://rpgboss.com/webstart/" href={fileName}>
                <information>
                    <title>rpgboss</title>
                    <vendor>rpgboss</vendor>
                    <description>rpgboss game editor and player</description>
                    <icon href="my_icon.png"/>
                    <icon href="my_splash.png" kind="splash"/>
                </information>
                <security>
                    <all-permissions/>
                </security> 
                <resources>
                    <j2se version="1.6+" max-heap-size="192m"/>
                    { assets map { _.toElem } }
                </resources>
                <application-desc main-class="rpgboss.editor.RpgDesktop"/>
            </jnlp>
        }
    ))
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
    val commonFilter = 
      new ExactFilter("gdx.jar") |
      new ExactFilter("extensions/gdx-freetype/gdx-freetype.jar") |
      new ExactFilter("extensions/gdx-audio/gdx-audio.jar") |
      new ExactFilter("sources/gdx-sources.jar")
    IO.unzip(zipFile, commonDest, commonFilter)

    val desktopDest = file("player-desktop/lib")
    val desktopFilter = 
      new ExactFilter("gdx-natives.jar") |
      new ExactFilter("gdx-backend-lwjgl.jar") |
      new ExactFilter("gdx-backend-lwjgl-natives.jar") |
      new ExactFilter("gdx-tools.jar") |
      new ExactFilter("extensions/gdx-freetype/gdx-freetype-natives.jar") |
      new ExactFilter("extensions/gdx-audio/gdx-audio-natives.jar") |
      new ExactFilter("sources/gdx-backend-lwjgl-sources.jar") |
      new ExactFilter("sources/gdx-openal-sources.jar")
    IO.unzip(zipFile, desktopDest, desktopFilter)
    // Put desktop backend in common for testing purposes.
    IO.unzip(zipFile, commonDest, desktopFilter)

    val androidDest = file("player-android/src/main/libs")
    val androidFilter = 
      new ExactFilter("gdx-backend-android.jar") |
      new ExactFilter("armeabi/libandroidgl20.so") |
      new ExactFilter("armeabi/libgdx.so") |
      new ExactFilter("extensions/gtx-audio/armeabi/libgdx-audio.so") |
      new ExactFilter("extensions/gdx-freetype/armeabi/libgdx-freetype.so") |
      new ExactFilter("armeabi-v7a/libgdx.so") |
      new ExactFilter("armeabi-v7a/libandroidgl20.so") |
      new ExactFilter("extensions/gdx-audio/armeabi-v7a/libgdx-audio.so") |
      new ExactFilter("extensions/gdx-freetype/armeabi-v7a/libgdx-freetype.so")
    
    IO.unzip(zipFile, androidDest, androidFilter)

    // Destroy the file.
    zipFile.delete
    s.log.info("Complete")
  }

  val updateLibs = TaskKey[Unit]("update-libs", "Updates libs")
  
  val updateLibsTask = updateLibs <<= streams map { (s: TaskStreams) =>
    import Process._
    import java.io._
    import java.net.URL

    val zipName = "tween-engine-api-6.3.3.zip"
    val zipFile = new File(zipName)
    val url = new URL("https://java-universal-tween-engine.googlecode.com/" + 
                      "files/" + zipName)
    IO.download(url, zipFile)
    IO.unzip(zipFile, file("common/lib"))

    zipFile.delete
  }
}

object LibgdxBuild extends Build {
  val common = Project (
    "common",
    file("common"),
    settings = Settings.common
  )

  lazy val playerDesktop = Project (
    "player-desktop",
    file("player-desktop"),
    settings = Settings.playerDesktop
  ) dependsOn common

  lazy val playerAndroid = Project (
    "player-android",
    file("player-android"),
    settings = Settings.playerAndroid
  ) dependsOn common
  
  lazy val editor = Project (
    "editor",
    file("editor"),
    settings = Settings.editor
  ) dependsOn playerDesktop
}
