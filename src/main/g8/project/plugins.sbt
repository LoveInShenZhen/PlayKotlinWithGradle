// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "$PlayVersion$")

// addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "$PlayEbeanVersion$")

libraryDependencies += "org.jodd" % "jodd-core" % "3.7"
