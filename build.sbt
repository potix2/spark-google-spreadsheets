enablePlugins(SparkPlugin)

name := "spark-google-spreadsheets"

organization := "io.github.kostjas"

homepage := Some(url("https://github.com/kostjas/spark-google-spreadsheets"))

organizationHomepage := Some(url("https://github.com/kostjas"))

description := "Google Spreadsheets datasource for SparkSQL and DataFrames."

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

scalaVersion := "2.12.15"

sparkVersion := "3.3.0"

sparkComponents := Seq("core", "sql")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.36" % "provided",
  "org.scalatest" %% "scalatest" % "3.2.13" % "test",
  "com.google.apis" % "google-api-services-sheets" % "v4-rev20220620-2.0.0" excludeAll(
    ExclusionRule("com.google.guava", "guava")
  ),
  "com.google.auth" % "google-auth-library-oauth2-http" % "1.10.0" excludeAll(
    ExclusionRule("com.google.guava", "guava")
  ),
  "org.scala-lang" % "scala-library" % scalaVersion.value % "compile",
  "javax.servlet" % "javax.servlet-api" % "4.0.1" % "compile",
  "com.google.guava" % "guava" % "31.1-jre"
)

resolvers ++= Seq(Resolver.mavenLocal, Resolver.sonatypeRepo("staging"))

/**
 * release settings
 */
publishMavenStyle := true

pgpKeyRing := Some(file("~/.gnupg/pubring.kbx"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

Test / publishArtifact := false

// Remove all additional repository other than Maven Central from POM
pomIncludeRepository := { _ => false }

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

// For all Sonatype accounts created on or after February 2021
sonatypeCredentialHost := "s01.oss.sonatype.org"

sonatypeProfileName := "io.github.kostjas"

publishTo := sonatypePublishToBundle.value

scmInfo := Some(
  ScmInfo(
    url("https://github.com/kostjas/spark-google-spreadsheets"),
    "git@github.com:kostjas/spark-google-spreadsheets.git"
  )
)

developers := List(
  Developer(
    id    = "kostjas",
    name  = "Kostya Spitsyn",
    email = "konstantin.spitsyn@gmail.com",
    url   = url("https://github.com/kostjas/")
  )
)

// Skip tests during assembly
assembly / test := {}

releaseCrossBuild := false

import ReleaseTransformations._

// Add publishing to spark packages as another step.
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

releaseVcsSign := true