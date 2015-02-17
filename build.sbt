name := """vamp-pulse-poc"""

version := "0.1"

scalaVersion := "2.11.4"

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M2",
  "org.scala-lang.modules" %% "scala-async" % "0.9.2",
  "org.glassfish.jersey.core" % "jersey-client" % "2.15",
  "org.glassfish.jersey.media" % "jersey-media-sse" % "2.15",
  "com.typesafe" % "config" % "1.2.1",
  "com.sclasen" %% "akka-kafka" % "0.1.0",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.10" % "compile",
  "org.elasticsearch" % "elasticsearch" % "1.4.3",
  "commons-io" % "commons-io" % "2.4"
)