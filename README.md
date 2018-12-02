# Spark Google Spreadsheets

Google Spreadsheets datasource for [SparkSQL and DataFrames](http://spark.apache.org/docs/latest/sql-programming-guide.html)

[![Build Status](https://travis-ci.org/potix2/spark-google-spreadsheets.svg?branch=master)](https://travis-ci.org/potix2/spark-google-spreadsheets)

## Notice

The version 0.4.0 breaks compatibility with previous version. You must
use a ** spreadsheetId ** to identify which spreadsheets is to be accessed or altered.
On older versions a spreadsheet name is used.

If you don't know spreadsheetId, please read the [Introduction to the Google Sheets API v4](https://developers.google.com/sheets/guides/concepts).

## Requirements

This library has different versions of Spark 1.6+, and 2.0+:

### Latest compatible versions

| This library | Spark Version |
| ------------ | ------------- |
| 0.6.x        | 2.3.x, 2.4.x  |
| 0.5.x        | 2.0.x         |
| 0.4.x        | 1.6.x         |

## Linking

Using SBT:

```
libraryDependencies += "com.github.potix2" %% "spark-google-spreadsheets" % "0.6.1"
```

Using Maven:

```xml
<dependency>
  <groupId>com.github.potix2</groupId>
  <artifactId>spark-google-spreadsheets_2.11</artifactId>
  <version>0.6.1</version>
</dependency>
```

## SQL API

```sql
CREATE TABLE cars
USING com.github.potix2.spark.google.spreadsheets
OPTIONS (
    path "<spreadsheetId>/worksheet1",
    serviceAccountId "xxxxxx@developer.gserviceaccount.com",
    credentialPath "/path/to/credentail.p12"
)
```

## Scala API

```scala
import org.apache.spark.sql.SQLContext

val sqlContext = new SQLContext(sc)

// Creates a DataFrame from a specified worksheet
val df = sqlContext.read.
    format("com.github.potix2.spark.google.spreadsheets").
    option("serviceAccountId", "xxxxxx@developer.gserviceaccount.com").
    option("credentialPath", "/path/to/credentail.p12").
    load("<spreadsheetId>/worksheet1")

// Saves a DataFrame to a new worksheet
df.write.
    format("com.github.potix2.spark.google.spreadsheets").
    option("serviceAccountId", "xxxxxx@developer.gserviceaccount.com").
    option("credentialPath", "/path/to/credentail.p12").
    save("<spreadsheetId>/newWorksheet")

```

## License

Copyright 2016-2018, Katsunori Kanda

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
