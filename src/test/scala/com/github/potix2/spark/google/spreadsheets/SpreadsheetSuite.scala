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
    sqlContext = new SQLContext(new SparkContext("local[2]", "AvroSuite"))
  }

  after {
    sqlContext.sparkContext.stop()
  }

  def withEmptyWorksheet(testCode:(String) => Any): Unit = {
    def deleteWorksheet(spreadSheetName: String, worksheetName: String): Unit = {
      implicit val spreadSheetContext = SparkSpreadsheetService(serviceAccountId, new File(testCredentialPath))
      for {
        s <- SparkSpreadsheetService.findSpreadsheet(spreadSheetName)
        w <- s.findWorksheet(worksheetName)
      } yield w.entry.delete()
    }

    val workSheetName = Random.alphanumeric.take(16).mkString
    try {
      testCode(workSheetName)
    }
    finally {
      deleteWorksheet("SpreadsheetSuite", workSheetName)
    }
  }

  "A sheet" should "behave as a dataFrame" in {
    val results = sqlContext
    .sheet(
        serviceAccountId,
        testCredentialPath,
        "SpreadsheetSuite",
        "case1")
    .select("col1")
    .collect()

    assert(results.size === 15)
  }

  it should "create from DDL" in {
    sqlContext.sql(
      s"""
         |CREATE TEMPORARY TABLE SpreadsheetSuite
         |USING com.github.potix2.spark.google.spreadsheets
         |OPTIONS (spreadsheet "SpreadsheetSuite", worksheet "case2", serviceAccountId "$serviceAccountId", credentialPath "$testCredentialPath")
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
    withEmptyWorksheet { workSheetName =>
      personsDF.saveAsSheet("SpreadsheetSuite", Map(
        "serviceAccountId" -> serviceAccountId,
        "credentialPath" -> testCredentialPath,
        "worksheetName" -> workSheetName))

      val result = sqlContext.sheet(serviceAccountId, testCredentialPath, "SpreadsheetSuite", workSheetName).collect()

      assert(result.size == 3)
      assert(result(0).getString(0) == "1")
      assert(result(0).getString(1) == "Kathleen")
      assert(result(0).getString(2) == "Cole")
    }
  }
}
