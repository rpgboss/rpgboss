seq(ProguardPlugin.proguardSettings :_*)

name := "rpgboss-editor"

version := "0.1"

organization := "rpgboss"

scalaVersion := "2.9.0-1"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-swing" % "2.9.0-1",
  "org.apache.httpcomponents" % "httpclient" % "4.1.1",
  "org.apache.sanselan" % "sanselan" % "0.97-incubator",
  "com.google.protobuf" % "protobuf-java" % "2.4.1",
  "net.java.dev.designgridlayout" % "designgridlayout" % "1.8",
  "com.google.guava" % "guava" % "10.0"
)

mainClass in (Compile, run) := Some("rpgboss.rpgapplet.RpgDesktop")

scalacOptions ++= List("-deprecation", "-Xexperimental")

proguardOptions ++= List(
  "-dontshrink",
  "-keep class rpgboss.rpgapplet.RpgApplet",
  """-keepclasseswithmembers public class * {
       public static void main(java.lang.String[]);
  }""")
