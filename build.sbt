name := "rpglib"

version := "0.1"

organization := "rpgboss"

scalaVersion := "2.9.0-1"

libraryDependencies ++= Seq(
  "postgresql" % "postgresql" % "9.0-801.jdbc4",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "commons-codec" % "commons-codec" % "1.5",
  "net.liftweb" %% "lift-json" % "2.4-M1" % "compile"
)

