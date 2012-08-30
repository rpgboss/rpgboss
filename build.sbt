name := "rpglib"

version := "0.1"

organization := "rpgboss"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
  "postgresql" % "postgresql" % "9.0-801.jdbc4",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.google.guava" % "guava" % "10.0",
  "net.liftweb" % "lift-json_2.9.1" % "2.4-M4",
  "net.iharder" % "base64" % "2.3.8",
  "com.weiglewilczek.slf4s" % "slf4s_2.9.1" % "1.0.7",
  "org.scalatest" %% "scalatest" % "1.6.1" % "test"
)

scalacOptions += "-deprecation"

