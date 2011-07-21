import sbt._
import Process._

class RpgApplet(info: ProjectInfo) 
extends DefaultProject(info) with ProguardProject
{
  val liftVersion = "2.4-M1"
  
  override def mainClass = 
    Some("rpgboss.rpgapplet.RpgDesktop")
  
  override def proguardInJars = super.proguardInJars +++ scalaLibraryPath
  
  override def proguardOptions = List(
    "-dontshrink",
    "-keep class rpgboss.rpgapplet.ui.RpgApplet",
    """-keepclasseswithmembers public class * {
      public static void main(java.lang.String[]);
    }"""
  )

  override def libraryDependencies = Set(
    "org.scala-lang" % "scala-swing" % "2.9.0-1",
    "net.liftweb" %% "lift-json" % liftVersion % "compile",
    "org.apache.httpcomponents" % "httpclient" % "4.1.1"
  ) ++ super.libraryDependencies
  
  lazy val applet = task {
    // normalize jar first
    "pack200 --repack %s".format(minJarPath) ! log
    
    """jarsigner -keystore keystore -storepass rpgboss 
    %s rpgboss""".format(minJarPath) ! log
    
    "pack200 %s %s".format(minJarPath + ".pack.gz", minJarPath) ! log
    
    None
  } dependsOn(proguard)
}

