name := """vamp-pulse"""

version := "0.5.0"

scalaVersion := "2.11.5"


resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
  Resolver.mavenLocal
)
//TODO: Look into slf4j dependency issues, that is solved now by log4j-over-slf4j.
// I think the problem is akka-streams conflicting with akka-kafka

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M3",
  "com.typesafe.akka" %% "akka-http-experimental" % "1.0-M3",
  "com.typesafe.akka" %% "akka-http-core-experimental" % "1.0-M3",
  "org.scala-lang.modules" %% "scala-async" % "0.9.2",
  "org.glassfish.jersey.core" % "jersey-client" % "2.15",
  "org.glassfish.jersey.media" % "jersey-media-sse" % "2.15",
  "com.typesafe" % "config" % "1.2.1",
  "com.sclasen" %% "akka-kafka" % "0.1.0",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.10" % "compile",
  "org.elasticsearch" % "elasticsearch" % "1.4.3",
  "commons-io" % "commons-io" % "2.4",
  "org.scalatest" %% "scalatest" % "3.0.0-SNAP4" % "test",
  "org.specs2" %% "specs2" % "2.4.16" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.9" % "test",
  "io.magnetic.vamp-common" % "vamp-common" % "0.5.0"
)