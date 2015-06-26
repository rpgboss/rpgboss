import sbt._

import com.typesafe.sbteclipse.plugin.EclipsePlugin._
import Keys._
import sbtassembly.AssemblyKeys._
import scala.sys.process.{Process => SysProcess}
import java.io.PrintWriter

object Settings {
  lazy val common = Defaults.defaultSettings ++ Seq (
    fork := true, // For natives loading.
    version := "0.1",
    scalaVersion := "2.11.6",
    scalacOptions ++= List("-deprecation", "-unchecked"),
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.0.6",
      "com.google.guava" % "guava" % "17.0",
      "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
      "commons-io" % "commons-io" % "2.4",
      "net.sf.opencsv" % "opencsv" % "2.0" withSources(),
      "org.json4s" %% "json4s-native" % "3.2.11" withSources(),
      "org.scalatest" %% "scalatest" % "2.1.5" % "test",
      "org.mozilla" % "rhino" % "1.7R4",
      "org.scalaj" %% "scalaj-http" % "1.1.4",
      "javax.websocket" % "javax.websocket-api" % "1.1",
      "org.glassfish.tyrus.bundles" % "tyrus-standalone-client" % "1.10"
    ),
    unmanagedJars in Compile <<= baseDirectory map { base =>
      var baseDirectories = (base / "lib") +++ (base / "lib" / "extensions")
      var jars = baseDirectories ** "*.jar"
      jars.classpath
    },
    updateLibsTask,
    TaskKey[Unit]("generateEnum") := {
      def listSubfiles(dir: File): Seq[File] = {
        assert(dir.isDirectory)
        val (childDirs, childFiles) = dir.listFiles.partition(_.isDirectory)
        childFiles ++: childDirs.flatMap(listSubfiles)
      }

      for (subDir <- List("defaultrc", "testrc")) {
        val fullDir = new File("common/src/main/resources", subDir)
        val subFiles = listSubfiles(fullDir).filter(_.name != "enumerated.txt")
        val relativizedSubFiles = subFiles
          .map(fullDir.relativize(_).get)
          .map(_.toString.replace("\\", "/"))
          .sorted

        val outputFile = new File(fullDir, "enumerated.txt")

        val writer = new PrintWriter(outputFile)
        relativizedSubFiles.foreach(s => {
          writer.write(s)
          writer.write("\n")
        })
        writer.close()
      }
      Unit
    },
    Keys.`compile` <<= (Keys.`compile` in Compile) dependsOn TaskKey[Unit]("generateEnum"),
    Keys.`compile` <<= (Keys.`compile` in Compile) dependsOn TaskKey[Unit]("update-libs"),
    Keys.`package` <<= (Keys.`package` in Compile) dependsOn TaskKey[Unit]("generateEnum"),
    Keys.`package` <<= (Keys.`package` in Compile) dependsOn TaskKey[Unit]("update-libs"),
    Keys.`test` <<= (Keys.`test` in Test) dependsOn TaskKey[Unit]("generateEnum"),
    Keys.`test` <<= (Keys.`test` in Test) dependsOn TaskKey[Unit]("update-libs"),
    EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource,
    EclipseKeys.eclipseOutput := Some("eclipsetarget")
  )

  lazy val editor = Settings.common ++ editorLibs ++ editorAssembly
  
  lazy val editorLibs = Seq(
    scalaVersion := "2.11.6",
    libraryDependencies ++= Seq(
      "org.apache.commons" % "commons-compress" % "1.9",
      "org.scala-lang.modules" %% "scala-swing" % "1.0.1",
      "com.github.benhutchison" %% "scalaswingcontrib" % "1.5", 
      "net.java.dev.designgridlayout" % "designgridlayout" % "1.8",
      "net.lingala.zip4j" % "zip4j" % "1.3.2",
      "com.fifesoft" % "rsyntaxtextarea" % "2.5.3"
    ),
    unmanagedJars in Compile <<= baseDirectory map { base => 
      var baseDirectories = (base / "lib") +++ (base / "lib" / "extensions")
      var jars = baseDirectories ** "*.jar"
      jars.classpath
    },
    mainClass in (Compile, run) := Some("rpgboss.editor.RpgDesktop"),
    scalacOptions ++= List("-deprecation", "-unchecked"),
    EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource,
    EclipseKeys.eclipseOutput := Some("eclipsetarget")
  )

  lazy val editorAssembly = Seq(
    assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly) { (old) =>
      {
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
    val gdxName = "libgdx-1.6.1"

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
      new ExactFilter("gdx-backend-headless.jar") |
      new ExactFilter("gdx-backend-lwjgl.jar") |
      new ExactFilter("gdx-backend-lwjgl-natives.jar") |
      new ExactFilter("gdx-tools.jar") |
      new ExactFilter("extensions/gdx-freetype/gdx-freetype-natives.jar")
    
    IO.unzip(gdxZipFile, commonDest, commonFilter)
    
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
