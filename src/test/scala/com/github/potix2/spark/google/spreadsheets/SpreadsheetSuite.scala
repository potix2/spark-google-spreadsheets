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

import java.io.File

import com.github.potix2.spark.google.spreadsheets.SparkSpreadsheetService.{SparkWorksheet, SparkSpreadsheetContext}
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SQLContext}
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.util.Random

class SpreadsheetSuite extends FlatSpec with BeforeAndAfter {
  private val serviceAccountId = "53797494708-ds5v22b6cbpchrv2qih1vg8kru098k9i@developer.gserviceaccount.com"
  private val testCredentialPath = "src/test/resources/spark-google-spreadsheets-test-eb7b191d1e1d.p12"

  private var sqlContext: SQLContext = _
  before {
    sqlContext = new SQLContext(new SparkContext("local[2]", "SpreadsheetSuite"))
  }

  after {
    sqlContext.sparkContext.stop()
  }

  private[spreadsheets] def deleteWorksheet(spreadSheetName: String, worksheetName: String)
                                           (implicit spreadSheetContext: SparkSpreadsheetContext): Unit = {
    for {
      s <- SparkSpreadsheetService.findSpreadsheet(spreadSheetName)
      w <- s.findWorksheet(worksheetName)
    } yield w.entry.delete()
  }

  private[spreadsheets] def addWorksheet(spreadSheetName: String, worksheetName: String)
                                        (implicit spreadSheetContext: SparkSpreadsheetContext):Option[SparkWorksheet] = {
    for {
      s <- SparkSpreadsheetService.findSpreadsheet(spreadSheetName)
      w <- Some(s.addWorksheet(worksheetName, 1000, 1000))
    } yield w
  }

  def withNewEmptyWorksheet(testCode:(String) => Any): Unit = {
    implicit val spreadSheetContext = SparkSpreadsheetService(serviceAccountId, new File(testCredentialPath))
    val workSheetName = Random.alphanumeric.take(16).mkString
    val worksheet = addWorksheet("SpreadsheetSuite", workSheetName)
    try {
      testCode(workSheetName)
    }
    finally {
      worksheet.get.entry.delete()
      //deleteWorksheet("SpreadsheetSuite", workSheetName)
    }
  }

  def withEmptyWorksheet(testCode:(String) => Any): Unit = {
    implicit val spreadSheetContext = SparkSpreadsheetService(serviceAccountId, new File(testCredentialPath))
    val workSheetName = Random.alphanumeric.take(16).mkString
    try {
      testCode(workSheetName)
    }
    finally {
      deleteWorksheet("SpreadsheetSuite", workSheetName)
    }
  }

  "A sheet" should "behave as a dataFrame" in {
    val results = sqlContext.read
      .option("serviceAccountId", serviceAccountId)
      .option("credentialPath", testCredentialPath)
      .spreadsheet("SpreadsheetSuite/case1")
      .select("col1")
      .collect()

    assert(results.size === 15)
  }

  it should "create from DDL" in {
    sqlContext.sql(
      s"""
         |CREATE TEMPORARY TABLE SpreadsheetSuite
         |USING com.github.potix2.spark.google.spreadsheets
         |OPTIONS (path "SpreadsheetSuite/case2", serviceAccountId "$serviceAccountId", credentialPath "$testCredentialPath")
       """.stripMargin.replaceAll("\n", " "))

    assert(sqlContext.sql("SELECT id, firstname, lastname FROM SpreadsheetSuite").collect().size == 10)
  }

  trait PersonDataFrame {
    val personsSchema = StructType(List(
      StructField("id", IntegerType, true),
      StructField("firstname", StringType, true),
      StructField("lastname", StringType, true)))
    val personsRows = Seq(Row(1, "Kathleen", "Cole"), Row(2, "Julia", "Richards"), Row(3, "Terry", "Black"))
    val personsRDD = sqlContext.sparkContext.parallelize(personsRows)
    val personsDF = sqlContext.createDataFrame(personsRDD, personsSchema)
  }

  "A dataFrame" should "save as a sheet" in new PersonDataFrame {
    import com.github.potix2.spark.google.spreadsheets._
    withEmptyWorksheet { workSheetName =>
      personsDF.write
        .option("serviceAccountId", serviceAccountId)
        .option("credentialPath", testCredentialPath)
        .spreadsheet(s"SpreadsheetSuite/$workSheetName")

      val result = sqlContext.read
        .option("serviceAccountId", serviceAccountId)
        .option("credentialPath", testCredentialPath)
        .spreadsheet(s"SpreadsheetSuite/$workSheetName")
        .collect()

      assert(result.size == 3)
      assert(result(0).getString(0) == "1")
      assert(result(0).getString(1) == "Kathleen")
      assert(result(0).getString(2) == "Cole")
    }
  }

  it should "create empty table" in {
    withNewEmptyWorksheet { worksheetName =>
      sqlContext.sql(
        s"""
           |CREATE TEMPORARY TABLE people
           |(id int, firstname string, lastname string)
           |USING com.github.potix2.spark.google.spreadsheets
           |OPTIONS (path "SpreadsheetSuite/$worksheetName", serviceAccountId "$serviceAccountId", credentialPath "$testCredentialPath")
       """.stripMargin.replaceAll("\n", " "))

      assert(sqlContext.sql("SELECT * FROM people").collect().size == 0)
    }
  }

}
