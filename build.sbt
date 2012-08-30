//seq(ProguardPlugin.proguardSettings :_*)

name := "rpgboss-editor"

version := "0.1"

organization := "rpgboss"

scalaVersion := "2.9.2"

fork in run := true

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-swing" % "2.9.2",
  "org.apache.httpcomponents" % "httpclient" % "4.1.1",
  "org.apache.sanselan" % "sanselan" % "0.97-incubator",
  "net.java.dev.designgridlayout" % "designgridlayout" % "1.8",
  "com.google.guava" % "guava" % "10.0",
  "net.liftweb" % "lift-json_2.9.1" % "2.4",
  "com.weiglewilczek.slf4s" % "slf4s_2.9.1" % "1.0.7",
  "ch.qos.logback" % "logback-classic" % "1.0.0"
)

mainClass in (Compile, run) := Some("rpgboss.editor.RpgDesktop")

scalacOptions ++= List("-deprecation", "-Xexperimental", "-unchecked")

//proguardOptions ++= List(
//  "-dontshrink",
//  "-keep class rpgboss.editor.RpgApplet",
//  """-keepclasseswithmembers public class * {
//       public static void main(java.lang.String[]);
//  }""")
