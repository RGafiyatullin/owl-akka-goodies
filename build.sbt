
name := "owl-akka-goodies"

organization := "com.github.rgafiyatullin"
version := "0.1.3"

scalaVersion in ThisBuild := "2.11.8"
val akkaVersion = "2.4.2"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

publishTo := {
  val nexus = "http://nexus.in-docker.localhost:8081/"
  Some("releases"  at nexus + "content/repositories/releases")
}
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")


libraryDependencies ++= Seq(
  "org.scalatest"       %% "scalatest"        % "2.2.6",

  "org.joda"            % "joda-convert"      % "1.7",
  "joda-time"           % "joda-time"         % "2.9.3",
  "com.typesafe.akka"   %% "akka-actor"       % akkaVersion,
  "com.typesafe.akka"   %% "akka-slf4j"       % akkaVersion,
  // "ch.qos.logback"      %  "logback-classic"  % "1.1.3",
  "com.google.inject"   %  "guice"            % "4.0-beta5",
  "net.codingwell"      %% "scala-guice"      % "4.0.0-beta5"
)

lazy val owlAkkaGoodies =
  Project("owl-akka-goodies", file("."))


