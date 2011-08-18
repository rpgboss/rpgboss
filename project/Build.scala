import sbt._
import Keys._
import Process._
import ProguardPlugin._

object RpgApplet extends Build
{
  val applet = TaskKey[Unit]("applet")
  
  val appletTask = applet <<= (proguard, minJarPath) map( (Unit, path) => {
    // normalize jar first
    "pack200 --repack %s".format(path) ! 
    
    """jarsigner -keystore keystore -storepass rpgboss 
    %s rpgboss""".format(path) ! 
    
    "pack200 %s %s".format(path + ".pack.gz", path) ! 
  })
  
  val scalaVer = "2.9.0-1" 
  val liftVersion = "2.4-M1"
  
  val buildSettings = Defaults.defaultSettings ++ Seq(
    name := "rpgapplet",
    version := "0.1",
    organization := "rpgboss",
    scalaVersion := scalaVer,
    libraryDependencies := Seq(
      "org.scala-lang" % "scala-swing" % scalaVer,
      "org.apache.httpcomponents" % "httpclient" % "4.1.1",
      "org.apache.sanselan" % "sanselan" % "0.97-incubator",
      "com.google.protobuf" % "protobuf-java" % "2.4.1"
    )
  )
  
  
  mainClass in (Compile, run) := Some("rpgboss.rpgapplet.RpgDesktop")
  
  lazy val project = Project("rpgapplet", file("."),
    settings = buildSettings ++ Seq(appletTask) ++ proguardSettings ++ Seq(
      proguardOptions ++= List(
                   "-dontshrink",
                   "-keep class rpgboss.rpgapplet.RpgApplet",
                   """-keepclasseswithmembers public class * {
                     public static void main(java.lang.String[]);
                   }""")
    )
  )
  
  
  

}

