
name := "owl-akka-goodies"

organization := "com.github.rgafiyatullin"
version := "0.1.9"

scalaVersion in ThisBuild := "2.11.8"
val akkaVersion = "2.5.4"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

//publishTo := {
//  val nexus = "http://am3-v-perftest-xmppcs-1.be.core.pw:8081/"
//  Some("releases"  at nexus + "content/repositories/sbt-releases")
//}
//credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishTo := {
  val nexus = "http://nexus.in-docker.localhost:8081/"
  Some("releases"  at nexus + "repository/my-releases")
}
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials.local")


libraryDependencies ++= Seq(
  "org.scalatest"       %% "scalatest"        % "2.2.6",

  "org.joda"            % "joda-convert"      % "1.7",
  "joda-time"           % "joda-time"         % "2.9.3",
  "com.typesafe.akka"   %% "akka-actor"       % akkaVersion
)

lazy val owlAkkaGoodies =
  Project("owl-akka-goodies", file("."))


