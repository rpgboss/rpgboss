resolvers += Resolver.url("scalasbt snapshots", new
        URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns)

resolvers += Resolver.url("sbt-plugin-releases-scalasbt", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.scala-sbt" % "xsbt-proguard-plugin" % "0.1.3")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.1.1")

addSbtPlugin("de.djini" % "xsbt-webstart" % "0.10.0")
