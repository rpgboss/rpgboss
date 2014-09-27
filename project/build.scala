import sbt._

import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._
import scala.sys.process.{Process => SysProcess}

object Settings {
  lazy val common = Defaults.defaultSettings ++ Seq (
    fork := true, // For natives loading.
    version := "0.1",
    scalaVersion := "2.11.2",
    scalacOptions ++= List("-deprecation", "-unchecked"),
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.0.6",
      "com.google.guava" % "guava" % "17.0",
      "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
      "net.sf.opencsv" % "opencsv" % "2.0" withSources(),
      "org.json4s" %% "json4s-native" % "3.2.10" withSources(),
      "org.scalatest" %% "scalatest" % "2.1.5" % "test",
      "org.mozilla" % "rhino" % "1.7R4"
    ),
    unmanagedJars in Compile <<= baseDirectory map { base =>
      var baseDirectories = (base / "lib") +++ (base / "lib" / "extensions")
      var jars = baseDirectories ** "*.jar"
      jars.classpath
    },
    updateLibsTask,
    TaskKey[Unit]("generateEnum") := {  
      SysProcess("python GenerateFileEnum.py", new File("common/src/main/resources")).run()
      println("Generated file enumeration")
      Unit
    },
    Keys.`compile` <<= (Keys.`compile` in Compile) dependsOn TaskKey[Unit]("generateEnum"),
    Keys.`compile` <<= (Keys.`compile` in Compile) dependsOn TaskKey[Unit]("update-libs"),
    Keys.`package` <<= (Keys.`package` in Compile) dependsOn TaskKey[Unit]("generateEnum"),
    Keys.`package` <<= (Keys.`package` in Compile) dependsOn TaskKey[Unit]("update-libs"),
    Keys.`test` <<= (Keys.`test` in Test) dependsOn TaskKey[Unit]("generateEnum"),
    Keys.`test` <<= (Keys.`test` in Test) dependsOn TaskKey[Unit]("update-libs")
   )

  lazy val editor = Settings.common ++ editorLibs ++ editorAssembly
  
  lazy val editorLibs = Seq(
    scalaVersion := "2.11.1",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-swing" % "1.0.1",
      "com.github.benhutchison" %% "scalaswingcontrib" % "1.5", 
      "org.apache.httpcomponents" % "httpclient" % "4.1.1",
      "net.java.dev.designgridlayout" % "designgridlayout" % "1.8"
    ),
    unmanagedJars in Compile <<= baseDirectory map { base => 
      var baseDirectories = (base / "lib") +++ (base / "lib" / "extensions")
      var jars = baseDirectories ** "*.jar"
      jars.classpath
    },
    mainClass in (Compile, run) := Some("rpgboss.editor.RpgDesktop"),
    scalacOptions ++= List("-deprecation", "-unchecked"))

  lazy val editorAssembly = assemblySettings ++ Seq(
    mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
      {
        case PathList("scala", "reflect", "internal", _ @ _*) => MergeStrategy.discard
        case PathList("scala", "tools", "nsc", _ @ _*) => MergeStrategy.discard
        case x => old(x)
      }
    }
  )

  val updateLibs = TaskKey[Unit]("update-libs", "Updates libs")
  
  val updateLibsTask = updateLibs <<= streams map { (s: TaskStreams) =>
    import Process._
    import java.io._
    import java.net.URL
    
    def downloadIfNeeded(filename: String, file: File, url: URL) = {
      if (file.exists) {
        s.log.info("%s already up to date.".format(filename))
      } else {
        val tempFile = File.createTempFile(filename, null)
        
        s.log.info("Pulling %s" format(filename))
        s.log.warn("This may take a few minutes...")
        IO.download(url, tempFile)
        IO.move(tempFile, file)
      }
    }
    
    val downloadDir = file("downloaded-libs")
    IO.createDirectory(downloadDir)
    
    // Delete and remake directory
    IO.delete(file("common/lib"))
    IO.createDirectory(file("common/lib"))    
    
    // Declare names
    val gdxBaseUrl = "http://libgdx.badlogicgames.com/releases"
    val gdxName = "libgdx-1.2.0"

    // Fetch the file.
    val gdxZipName = "%s.zip" format(gdxName)
    val gdxZipFile = new java.io.File(downloadDir, gdxZipName)
    val gdxUrl = new URL("%s/%s" format(gdxBaseUrl, gdxZipName))
    
    downloadIfNeeded(gdxZipName, gdxZipFile, gdxUrl)

    // Extract jars into their respective lib folders.
    val commonDest = file("common/lib")
    val commonFilter = 
      new ExactFilter("gdx.jar") |
      new ExactFilter("extensions/gdx-freetype/gdx-freetype.jar") |
      new ExactFilter("extensions/gdx-audio/gdx-audio.jar") |
      new ExactFilter("gdx-natives.jar") |
      new ExactFilter("gdx-backend-lwjgl.jar") |
      new ExactFilter("gdx-backend-lwjgl-natives.jar") |
      new ExactFilter("gdx-tools.jar") |
      new ExactFilter("extensions/gdx-freetype/gdx-freetype-natives.jar")
    
    IO.unzip(gdxZipFile, commonDest, commonFilter)
    
    val tweenZipName = "tween-engine-api-6.3.3.zip"
    
    val tweenZipFile = new File(downloadDir, tweenZipName)
    val tweenUrl = new URL(
      "https://java-universal-tween-engine.googlecode.com/" + 
      "files/" + tweenZipName)
    
    downloadIfNeeded(tweenZipName, tweenZipFile, tweenUrl)
    IO.unzip(tweenZipFile, file("common/lib"))
    
    s.log.info("Complete")
  }
}

object LibgdxBuild extends Build {
  val common = Project (
    "common",
    file("common"),
    settings = Settings.common
  )

  lazy val editor = Project (
    "editor",
    file("editor"),
    settings = Settings.editor
  ) dependsOn common

  lazy val root = Project("root", file("."))
    .aggregate(common, editor)
    .settings(
      run := {
        (run in editor in Compile).evaluated
      }//,
//      test := {
//        (test in common)
//      }
    )

}
