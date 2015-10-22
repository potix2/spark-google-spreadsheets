name := "spark-google-spreadsheets"

organization := "com.potix2"

scalaVersion := "2.10.6"

crossScalaVersions := Seq("2.10.6", "2.11.7")

version := "0.1.0"

spName := "potix2/spark-google-spreadsheets"

spAppendScalaVersion := true

spIncludeMaven := true

spIgnoreProvided := true

sparkVersion := "1.4.1"

val testSparkVersion = settingKey[String]("The version of Spark to test against.")

testSparkVersion := sys.props.get("spark.testVersion").getOrElse(sparkVersion.value)

sparkComponents := Seq("sql")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.5" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.google.api-client" % "google-api-client" % "1.20.0" % "provided",
  "com.google.gdata" % "core" % "1.47.1" % "provided"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % testSparkVersion.value,
  "org.apache.spark" %% "spark-sql" % testSparkVersion.value
)

/**
 * release settings
 */
publishMavenStyle := true

releaseCrossBuild := true

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/potix2/spark-google-spreadsheets</url>
  <licenses>
    <license>
      <name>Apache License, Verision 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:potix2/spark-google-spreadsheets.git</url>
    <connection>scm:git:git@github.com:potix2/spark-google-spreadsheets.git</connection>
  </scm>
  <developers>
    <developer>
      <id>potix2</id>
      <name>Katsunori Kanda</name>
      <url>https://github.com/potix2/</url>
    </developer>
  </developers>)

ScoverageSbtPlugin.ScoverageKeys.coverageHighlighting := {
  if (scalaBinaryVersion.value == "2.10") false
  else true
}

import ReleaseTransformations._

// Add publishing to spark packages as another step.
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges,
  releaseStepTask(spPublish)
)
