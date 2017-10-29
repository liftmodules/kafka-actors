name := "kafka-actors"

version := "0.1.0"

organization := "net.liftmodules"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.11", "2.12.4")

val liftVersion = settingKey[String]("Lift Web Framework full version number")
val liftEdition = settingKey[String]("Lift Edition (such as 2.6 or 3.0)")
val kafkaVersion = settingKey[String]("Version of Kafka")

liftVersion  := "3.1.1"
liftEdition  := (liftVersion.apply(_.substring(0,3))).value
kafkaVersion := "0.11.0.1"

moduleName := name.value + "_" + liftEdition.value

libraryDependencies := Seq(
  "net.liftweb"         %% "lift-actor"   % liftVersion.value   % "provided",
  "net.liftweb"         %% "lift-json"    % liftVersion.value   % "provided",
  "org.apache.kafka"    % "kafka-clients" % kafkaVersion.value,
  "org.scalactic"       %% "scalactic"    % "3.0.4"             % "test",
  "org.scalatest"       %% "scalatest"    % "3.0.4"             % "test",
  "ch.qos.logback"      %  "logback-classic" % "1.2.2" % "test"
)

scalacOptions ++= Seq("-unchecked", "-deprecation")

pomExtra := {
  <url>https://github.com/liftmodules/json-extractor-ng</url>
  <licenses>
    <license>
      <name>Apache 2.0 License</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:liftmodules/json-extractor-ng.git</url>
    <connection>scm:git:git@github.com:liftmodules/json-extractor-ng.git</connection>
  </scm>
  <developers>
    <developer>
      <id>liftmodules</id>
      <name>Lift Team</name>
      <url>http://www.liftmodules.net</url>
    </developer>
  </developers>
}
