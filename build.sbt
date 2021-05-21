enablePlugins(SparkPlugin)

name := "spark-google-spreadsheets"

organization := "com.github.kostjas"

scalaVersion := "2.12.13"

version := "0.10.0-SNAPSHOT"

sparkVersion := "3.1.1"

val testSparkVersion = settingKey[String]("The version of Spark to test against.")

testSparkVersion := sys.props.get("spark.testVersion").getOrElse(sparkVersion.value)

sparkComponents := Seq("sql")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.30" % "provided",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test",
  ("com.google.api-client" % "google-api-client" % "1.31.5").
    exclude("com.google.guava", "guava-jdk5")
    .exclude("com.google.guava", "guava:30.1.1-android"),
  "com.google.apis" % "google-api-services-sheets" % "v4-rev20210322-1.31.0"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % testSparkVersion.value % "test" force(),
  "org.apache.spark" %% "spark-sql" % testSparkVersion.value % "test"  force(),
  "org.scala-lang" % "scala-library" % scalaVersion.value % "compile",
  "javax.servlet" % "javax.servlet-api" % "4.0.1" % "compile"
)

/**
 * release settings
 */
publishMavenStyle := true

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
  <url>https://github.com/kostjas/spark-google-spreadsheets</url>
  <scm>
    <url>git@github.com:kostjas/spark-google-spreadsheets.git</url>
    <connection>scm:git:git@github.com:kostjas/spark-google-spreadsheets.git</connection>
  </scm>
  <developers>
    <developer>
      <id>kostjas</id>
      <name>KostjaS</name>
      <url>https://github.com/kostjas/</url>
    </developer>
  </developers>)

// Skip tests during assembly
test in assembly := {}

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
  pushChanges
)
