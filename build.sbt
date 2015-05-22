import _root_.sbt.Keys._


organization in ThisBuild := "io.vamp"

name := """pulse"""

version in ThisBuild := "0.7.6"

scalaVersion := "2.11.6"

scalaVersion in ThisBuild := scalaVersion.value

publishMavenStyle := true

// This has to be overridden for sub-modules to have different description
description := """Pulse is an event consumption/retrieval/aggregation application"""

pomExtra in ThisBuild := <url>http://vamp.io</url>
  <licenses>
    <license>
      <name>The Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
    </license>
  </licenses>
  <developers>
    <developer>
      <name>Dragoslav Pavkovic</name>
      <email>drago@magnetic.io</email>
      <organization>VAMP</organization>
      <organizationUrl>http://vamp.io</organizationUrl>
    </developer>
    <developer>
      <name>Roman Useinov</name>
      <email>roman@magnetic.io</email>
      <organization>VAMP</organization>
      <organizationUrl>http://vamp.io</organizationUrl>
    </developer>
  </developers>
  <scm>
    <connection>scm:git:git@github.com:magneticio/vamp-pulse.git</connection>
    <developerConnection>scm:git:git@github.com:magneticio/vamp-pulse.git</developerConnection>
    <url>git@github.com:magneticio/vamp-pulse.git</url>
  </scm>

//
resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
  "spray repo" at "http://repo.spray.io",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  Resolver.jcenterRepo
)

// Shared dependencies

val vampCommonV = "0.7.6"
val json4sV = "3.2.11"

// Note ThisBuild, this is what makes these dependencies shared
libraryDependencies in ThisBuild ++= Seq(
  "com.typesafe.akka" %% "akka-testkit" % "2.3.9" % "test","io.vamp" %% "common" % vampCommonV,
  "org.json4s" %% "json4s-core" % json4sV,
  "org.json4s" %% "json4s-ext" % json4sV,
  "org.json4s" %% "json4s-native" % json4sV
)

// Force scala version for the dependencies
dependencyOverrides in ThisBuild ++= Set(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang" % "scala-library" % scalaVersion.value
)

// Root project and subproject definitions
lazy val root = project.in(file(".")).settings(
  // Disable publishing root empty pom
  packagedArtifacts in file(".") := Map.empty,
  // allows running main classes from subprojects
  run := {
    (run in server in Compile).evaluated
  }
).aggregate(
    server, model, client
  ).disablePlugins(sbtassembly.AssemblyPlugin)

val akkaVersion = "2.3.11"
val akkaStreamsVersion = "1.0-M3"
val sprayVersion = "1.3.2"
val jerseyVersion = "2.15"
val jacksonVersion = "2.5.0"
val scalaLoggingVersion = "3.1.0"
val slf4jVersion = "1.7.10"
val logbackVersion = "1.1.2"

lazy val server = project.settings(
  libraryDependencies ++=Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream-experimental" % akkaStreamsVersion,
    "org.scala-lang.modules" %% "scala-async" % "0.9.2",
    "org.glassfish.jersey.core" % "jersey-client" % jerseyVersion,
    "org.glassfish.jersey.media" % "jersey-media-sse" % jerseyVersion,
    "com.typesafe" % "config" % "1.2.1",
    "com.sclasen" %% "akka-kafka" % "0.1.0",
    "commons-io" % "commons-io" % "2.4",
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-http" % sprayVersion,
    "io.spray" %% "spray-util" % sprayVersion,
    "io.spray" %% "spray-io" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "io.spray" %% "spray-can" % sprayVersion,
    "com.github.nscala-time" %% "nscala-time" % "1.8.0",
    "com.sksamuel.elastic4s" %% "elastic4s" % "1.5.4",
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "org.scalatest" %% "scalatest" % "2.2.5" % "test"
  )
).dependsOn(model).disablePlugins(sbtassembly.AssemblyPlugin)

lazy val model = project.disablePlugins(sbtassembly.AssemblyPlugin)

lazy val client = project.dependsOn(model).disablePlugins(sbtassembly.AssemblyPlugin)

scalacOptions in ThisBuild ++= Seq(Opts.compile.deprecation, Opts.compile.unchecked) ++
  Seq("-target:jvm-1.8", "-Ywarn-unused-import", "-Ywarn-unused", "-Xlint", "-feature")

javacOptions ++= Seq("-encoding", "UTF-8")


bintrayPublishSettings

bintray.Keys.repository in bintray.Keys.bintray := "vamp"

licenses  += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

bintray.Keys.bintrayOrganization in bintray.Keys.bintray := Some("magnetic-io")


