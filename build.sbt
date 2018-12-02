name := "spark-google-spreadsheets"

organization := "com.github.potix2"

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.11.12")

version := "0.6.2-SNAPSHOT"

spName := "potix2/spark-google-spreadsheets"

spAppendScalaVersion := true

spIncludeMaven := true

spIgnoreProvided := true

sparkVersion := "2.3.1"

val testSparkVersion = settingKey[String]("The version of Spark to test against.")

testSparkVersion := sys.props.get("spark.testVersion").getOrElse(sparkVersion.value)

sparkComponents := Seq("sql")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.5" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  ("com.google.api-client" % "google-api-client" % "1.22.0").
    exclude("com.google.guava", "guava-jdk5"),
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.22.0",
  "com.google.apis" % "google-api-services-sheets" % "v4-rev18-1.22.0"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % testSparkVersion.value % "test" force(),
  "org.apache.spark" %% "spark-sql" % testSparkVersion.value % "test"  force(),
  "org.scala-lang" % "scala-library" % scalaVersion.value % "compile",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % "compile"
)

/**
 * release settings
 */
publishMavenStyle := true

releaseCrossBuild := true

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/potix2/spark-google-spreadsheets</url>
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

// Skip tests during assembly
test in assembly := {}

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
