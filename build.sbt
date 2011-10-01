import sbtprotobuf.{SbtProtobufPlugin=>PB}

seq(PB.protobufSettings: _*)

name := "rpglib"

version := "0.1"

organization := "rpgboss"

scalaVersion := "2.9.0-1"

libraryDependencies ++= Seq(
  "postgresql" % "postgresql" % "9.0-801.jdbc4",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.google.guava" % "guava" % "10.0" 
)

scalacOptions += "-deprecation"

