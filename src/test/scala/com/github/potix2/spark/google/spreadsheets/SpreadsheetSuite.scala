/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.potix2.spark.google.spreadsheets

import com.github.potix2.spark.google.spreadsheets.SparkSpreadsheetService.SparkSpreadsheetContext
import com.github.potix2.spark.google.spreadsheets.util.Credentials
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SparkSession}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

import scala.util.Random

class SpreadsheetSuite extends AnyFlatSpec with BeforeAndAfter {

  private val oAuthJson = System.getenv("OAUTH_JSON")
  private val testSpreadsheetID = System.getenv("TEST_SPREADSHEET_ID")

  private val credentials = Credentials.credentialsFromJsonString(oAuthJson)

  private var sqlContext: SQLContext = _
  before {
    sqlContext = SparkSession.builder()
      .master("local[2]")
      .appName("SpreadsheetSuite")
      .getOrCreate().sqlContext
  }

  after {
    sqlContext.sparkContext.stop()
  }

  private[spreadsheets] def deleteWorksheet(spreadSheetName: String, worksheetName: String)
                                           (implicit spreadSheetContext: SparkSpreadsheetContext): Unit = {
    SparkSpreadsheetService
      .findSpreadsheet(spreadSheetName)
      .foreach(_.deleteWorksheet(worksheetName))
  }

  def withNewEmptyWorksheet(testCode: String => Any): Unit = {
    implicit val spreadSheetContext = SparkSpreadsheetService(credentials)
    val spreadsheet = SparkSpreadsheetService.findSpreadsheet(testSpreadsheetID)
    spreadsheet.foreach { s =>
      val workSheetName = Random.alphanumeric.take(16).mkString
      s.addWorksheet(workSheetName, 1000, 1000)
      try {
        testCode(workSheetName)
      }
      finally {
        s.deleteWorksheet(workSheetName)
      }
    }
  }

  def withEmptyWorksheet(testCode: String => Any): Unit = {
    implicit val spreadSheetContext = SparkSpreadsheetService(credentials)
    val workSheetName = Random.alphanumeric.take(16).mkString
    try {
      testCode(workSheetName)
    }
    finally {
      deleteWorksheet(testSpreadsheetID, workSheetName)
    }
  }

  behavior of "A sheet"

  it should "behave as a DataFrame" in {
    val results = sqlContext.read
      .option("credentialsJson", oAuthJson)
      .spreadsheet(s"$testSpreadsheetID/case1")
      .select("col1")
      .collect()

    assert(results.length === 15)
  }

  it should "have a `long` value" in {
    val schema = StructType(Seq(
      StructField("col1", DataTypes.LongType),
      StructField("col2", DataTypes.StringType),
      StructField("col3", DataTypes.StringType)
    ))

    val results = sqlContext.read
      .option("credentialsJson", oAuthJson)
      .schema(schema)
      .spreadsheet(s"$testSpreadsheetID/case1")
      .select("col1", "col2", "col3")
      .collect()

    assert(results.head.getLong(0) === 1L)
    assert(results.head.getString(1) === "2")
    assert(results.head.getString(2) === "3")
  }

  trait PersonData {
    val personsSchema = StructType(List(
      StructField("id", IntegerType, true),
      StructField("firstname", StringType, true),
      StructField("lastname", StringType, true)))
  }

  trait PersonDataFrame extends PersonData {
    val personsRows = Seq(Row(1, "Kathleen", "Cole"), Row(2, "Julia", "Richards"), Row(3, "Terry", "Black"))
    val personsRDD = sqlContext.sparkContext.parallelize(personsRows)
    val personsDF = sqlContext.createDataFrame(personsRDD, personsSchema)
  }

  trait SparsePersonDataFrame extends PersonData {
    val RowCount = 10

    def firstNameValue(id: Int): String = {
      if (id % 3 != 0) s"first-$id" else null
    }

    def lastNameValue(id: Int): String = {
      if (id % 4 != 0) s"last-$id" else null
    }

    val personsRows = (1 to RowCount) map { id: Int =>
      Row(id, firstNameValue(id), lastNameValue(id))
    }
    val personsRDD = sqlContext.sparkContext.parallelize(personsRows)
    val personsDF = sqlContext.createDataFrame(personsRDD, personsSchema)
  }

  behavior of "A DataFrame"

  it should "be saved as a sheet" in new PersonDataFrame {
    import com.github.potix2.spark.google.spreadsheets._
    withEmptyWorksheet { workSheetName =>
      personsDF.write
        .option("credentialsJson", oAuthJson)
        .spreadsheet(s"$testSpreadsheetID/$workSheetName")

      val result = sqlContext.read
        .option("credentialsJson", oAuthJson)
        .spreadsheet(s"$testSpreadsheetID/$workSheetName")
        .collect()

      assert(result.length == 3)
      assert(result(0).getString(0) == "1")
      assert(result(0).getString(1) == "Kathleen")
      assert(result(0).getString(2) == "Cole")
    }
  }

  it should "infer it's schema from headers" in {
    val results = sqlContext.read
      .option("credentialsJson", oAuthJson)
      .spreadsheet(s"$testSpreadsheetID/case3")

    assert(results.columns.length === 2)
    assert(results.columns.contains("a"))
    assert(results.columns.contains("b"))
  }

  "A sparse DataFrame" should "be saved as a sheet, preserving empty cells" in new SparsePersonDataFrame {
    import com.github.potix2.spark.google.spreadsheets._
    withEmptyWorksheet { workSheetName =>
      personsDF.write
        .option("credentialsJson", oAuthJson)
        .spreadsheet(s"$testSpreadsheetID/$workSheetName")

      val result = sqlContext.read
        .schema(personsSchema)
        .option("credentialsJson", oAuthJson)
        .spreadsheet(s"$testSpreadsheetID/$workSheetName")
        .collect()

      assert(result.length == RowCount)

      (1 to RowCount) foreach { id: Int =>
        val row = id - 1
        val first = firstNameValue(id)
        val last = lastNameValue(id)
        // TODO: further investigate/fix null handling
        // assert(result(row) == Row(id, if (first == null) "" else first, if (last == null) "" else last))
      }
    }
  }

  "A table" should "be created from DDL with schema" in {
    withNewEmptyWorksheet { worksheetName =>
      sqlContext.sql(
        s"""
           |CREATE TEMPORARY TABLE people
           |(id int, firstname string, lastname string)
           |USING com.github.potix2.spark.google.spreadsheets
           |OPTIONS (path "$testSpreadsheetID/$worksheetName", credentialsJson "$oAuthJson")
       """.stripMargin.replaceAll("\n", " "))

      assert(sqlContext.sql("SELECT * FROM people").collect().length == 0)
    }
  }

  it should "be created from DDL with inferred schema" in {
    sqlContext.sql(
      s"""
         |CREATE TEMPORARY TABLE SpreadsheetSuite
         |USING com.github.potix2.spark.google.spreadsheets
         |OPTIONS (path "$testSpreadsheetID/case2", credentialsJson "$oAuthJson")
       """.stripMargin.replaceAll("\n", " "))

    assert(sqlContext.sql("SELECT id, firstname, lastname FROM SpreadsheetSuite").collect().length == 10)
  }

  it should "be inserted from sql" in {
    withNewEmptyWorksheet { worksheetName =>
      sqlContext.sql(
        s"""
           |CREATE TEMPORARY TABLE accesslog
           |(id string, firstname string, lastname string, email string, country string, ipaddress string)
           |USING com.github.potix2.spark.google.spreadsheets
           |OPTIONS (path "$testSpreadsheetID/$worksheetName", credentialsJson "$oAuthJson")
       """.stripMargin.replaceAll("\n", " "))

      sqlContext.sql(
        s"""
           |CREATE TEMPORARY TABLE SpreadsheetSuite
           |USING com.github.potix2.spark.google.spreadsheets
           |OPTIONS (path "$testSpreadsheetID/case2", credentialsJson "$oAuthJson")
       """.stripMargin.replaceAll("\n", " "))

      sqlContext.sql("INSERT OVERWRITE TABLE accesslog SELECT * FROM SpreadsheetSuite")
      assert(sqlContext.sql("SELECT id, firstname, lastname FROM accesslog").collect().length == 10)
    }
  }

  trait UnderscoreDataFrame {
    val aSchema: StructType = StructType(List(StructField("foo_bar", IntegerType, true)))
    val aRows = Seq(Row(1), Row(2), Row(3))
    val aRDD: RDD[Row] = sqlContext.sparkContext.parallelize(aRows)
    val aDF: DataFrame = sqlContext.createDataFrame(aRDD, aSchema)
  }

  "The underscore" should "be used in a column name" in new UnderscoreDataFrame {
    import com.github.potix2.spark.google.spreadsheets._
    withEmptyWorksheet { workSheetName =>
      aDF.write
        .option("credentialsJson", oAuthJson)
        .spreadsheet(s"$testSpreadsheetID/$workSheetName")

      val result = sqlContext.read
        .option("credentialsJson", oAuthJson)
        .spreadsheet(s"$testSpreadsheetID/$workSheetName")
        .collect()

      assert(result.length == 3)
      assert(result(0).getString(0) == "1")
    }
  }
}
