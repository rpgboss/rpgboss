import scala.sys.process.{Process => SysProcess}

name := "rpgboss-defaultrc"

version := "0.1"

organization := "rpgboss"

scalaVersion := "2.9.1"

TaskKey[Unit]("generateEnum") := {  
  SysProcess("python GenerateFileEnum.py", new File("src/main/resources")).run()
  println("Generated file enumeration")
  Unit
}

Keys.`package` <<= (Keys.`package` in Compile) dependsOn TaskKey[Unit]("generateEnum")
