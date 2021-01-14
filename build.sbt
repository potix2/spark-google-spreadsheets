name := "spark-google-spreadsheets"

organization := "com.vinted"

scalaVersion := "2.12.12"

enablePlugins(GitVersioning)
git.useGitDescribe := true

spName := "potix2/spark-google-spreadsheets"
spAppendScalaVersion := true
spIncludeMaven := true
spIgnoreProvided := true

sparkVersion := "2.4.7"

val testSparkVersion = settingKey[String]("The version of Spark to test against.")
testSparkVersion := sys.props.get("spark.testVersion").getOrElse(sparkVersion.value)
sparkComponents := Seq("sql")

libraryDependencies ++= Seq(
  "com.google.api-client" % "google-api-client" % "1.31.1" exclude("com.google.guava", "guava-jdk5"),
  "com.google.oauth-client" % "google-oauth-client-jetty" % "1.31.4",
  "com.google.apis" % "google-api-services-sheets" % "v4-rev612-1.25.0",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.apache.spark" %% "spark-core" % testSparkVersion.value % "test" force(),
  "org.apache.spark" %% "spark-sql" % testSparkVersion.value % "test"  force()
)

val publishCredentials =
  (sys.env.get("NEXUS_USERNAME"), sys.env.get("NEXUS_PASSWORD")) match {
    case (Some(username), Some(password)) => username -> password
    case _ => "" -> ""
  }

credentials += Credentials("maven-hosted-oom", "nexus.vinted.net", publishCredentials._1, publishCredentials._2)

publishTo := Some("maven-hosted-oom" at "https://nexus.vinted.net/repository/maven-hosted-oom")
