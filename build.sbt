import sbt.Keys._

organization in ThisBuild := "io.vamp"

name := """pulse"""

version in ThisBuild := "0.7.8" //+ "." + GitHelper.headSha()

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
  Resolver.typesafeRepo("releases"),
  Resolver.jcenterRepo
)

lazy val bintraySetting = Seq(
  bintrayOrganization := Some("magnetic-io"),
  licenses +=("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  bintrayRepository := "vamp"
)

// Shared dependencies

val json4sVersion = "3.2.11"
val vampCommonVersion = "0.7.8"

// Note ThisBuild, this is what makes these dependencies shared
libraryDependencies in ThisBuild ++= Seq(
  "io.vamp" %% "common" % vampCommonVersion,
  "org.json4s" %% "json4s-core" % json4sVersion,
  "org.json4s" %% "json4s-ext" % json4sVersion,
  "org.json4s" %% "json4s-native" % json4sVersion
)

// Force scala version for the dependencies
dependencyOverrides in ThisBuild ++= Set(
  "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  "org.scala-lang" % "scala-library" % scalaVersion.value
)

// Root project and subproject definitions
lazy val root = project.in(file(".")).settings(bintraySetting: _*).settings(
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
val junitVersion = "4.11"

lazy val server = project.settings(bintraySetting: _*).settings(
  description := "Server for Vamp Pulse",
  name:="pulse-server",
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
    "com.sksamuel.elastic4s" %% "elastic4s" % "1.5.4",
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "ch.qos.logback" % "logback-classic" % logbackVersion,
    "org.scalatest" %% "scalatest" % "2.2.5" % "test",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
    "io.spray" %% "spray-testkit" % sprayVersion % "test"
  ),
  // Runnable assembly jar lives in server/target/scala_2.11/ and is renamed to pulse assembly for consistent filename for
  // downloading
  assemblyJarName in assembly := s"pulse-assembly-${version.value}.jar",
  assemblyExcludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp filter {_.data.getName == "joda-convert-1.6.jar"}
  }
).dependsOn(model, client)

lazy val model = project.settings(bintraySetting: _*).settings(
  description := "Model for Vamp Pulse",
  name:="pulse-model"
).disablePlugins(sbtassembly.AssemblyPlugin)

lazy val client = project.settings(bintraySetting: _*).settings(
  description := "Client for Vamp Pulse",
  name:="pulse-client"
).dependsOn(model).disablePlugins(sbtassembly.AssemblyPlugin)

scalacOptions in ThisBuild ++= Seq(Opts.compile.deprecation, Opts.compile.unchecked) ++
  Seq("-target:jvm-1.8", "-Ywarn-unused-import", "-Ywarn-unused", "-Xlint", "-feature")

javacOptions ++= Seq("-encoding", "UTF-8")