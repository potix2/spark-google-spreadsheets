# Spark Google Spreadsheets

Google Spreadsheets datasource for [SparkSQL and DataFrames](http://spark.apache.org/docs/latest/sql-programming-guide.html)

## Building
  * Go to the repo above and build jar: `sbt package`
  * And build pom: `sbt make-pom`

## Deployment
In order for dependency management to work properly, each deployed package must have a different version.  Increment the version in the pom file before building and deploying.

Deployment is currently manual.  Make sure that you have a user for myget with permissions to upload to the spark-google-spreadsheets-lingk feed (https://www.myget.org/feed/Packages/spark-google-spreadsheets-lingk).

From the "Packages" page of the feed:
  * Add Package -> Maven Package
  * choose $SPARK_SALESFORCE_HOME/target/scala-2.1.1/spark-google-spreadsheets_2.11-$VERSION.jar for the jar
  * choose $SPARK_SALESFORCE_HOME/target/scala-2.1.1/spark-google-spreadsheets_2.11-$VERSION.pom for the pom

## Notice

The version 0.4.0 breaks compatibility with previous versions. You must
use a ** spreadsheetId ** to identify which spreadsheet is to be accessed or altered.
In older versions, spreadsheet name was used.

If you don't know spreadsheetId, please read the [Introduction to the Google Sheets API v4](https://developers.google.com/sheets/guides/concepts).

## Requirements

This library supports different versions of Spark:

### Latest compatible versions

| This library | Spark Version |
| ------------ | ------------- |
| 0.6.x        | 2.3.x, 2.4.x  |
| 0.5.x        | 2.0.x         |
| 0.4.x        | 1.6.x         |

## Linking

Using SBT:

```
libraryDependencies += "com.github.potix2" %% "spark-google-spreadsheets" % "0.6.3"
```

Using Maven:

```xml
<dependency>
  <groupId>io.lingk</groupId>
  <artifactId>spark-google-spreadsheets_2.11</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Scala API

```scala
import org.apache.spark.sql.SQLContext

val sqlContext = new SQLContext(sc)

// Creates a DataFrame from a specified worksheet
val df = sqlContext.read.
    format("com.github.potix2.spark.google.spreadsheets").
    option("client_json", "credential_in_json_format").
    load("<spreadsheetId>/Sheet1")
git 
// Saves a DataFrame to a new worksheet
df.write.
    format("com.github.potix2.spark.google.spreadsheets").
    option("client_json", "credential_in_json_format").
    save("<spreadsheetId>/newWorksheet")

```

### Using Google default application credentials

Provide authentication credentials to your application code by setting the environment variable 
`GOOGLE_APPLICATION_CREDENTIALS`. The variable should be set to the path of the service account json file.


```scala
import org.apache.spark.sql.SQLContext

val sqlContext = new SQLContext(sc)

// Creates a DataFrame from a specified worksheet
val df = sqlContext.read.
    format("com.github.potix2.spark.google.spreadsheets").
    load("<spreadsheetId>/worksheet1")
```

More details: https://cloud.google.com/docs/authentication/production

## License

Copyright 2016-2018, Katsunori Kanda

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
