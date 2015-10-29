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

import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class SpreadsheetSuite extends FunSuite with BeforeAndAfterAll {
  private val serviceAccountId = "53797494708-ds5v22b6cbpchrv2qih1vg8kru098k9i@developer.gserviceaccount.com"
  private val testCredentialPath = "src/test/resources/spark-google-spreadsheets-test-eb7b191d1e1d.p12"

  private var sqlContext: SQLContext = _
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    sqlContext = new SQLContext(new SparkContext("local[2]", "AvroSuite"))
  }

  override protected def afterAll(): Unit = {
    try {
      sqlContext.sparkContext.stop()
    } finally {
      super.afterAll()
    }
  }

  test("DSL test: case1") {
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

  test("DSL test: case2") {
    val results = sqlContext
    .sheet(
        serviceAccountId,
        testCredentialPath,
        "SpreadsheetSuite",
        "case2")
    .select("id", "firstname", "lastname", "email")
    .collect()

    assert(results.size === 10)
  }

  test("DDL test") {
    sqlContext.sql(
      s"""
         |CREATE TEMPORARY TABLE SpreadsheetSuite
         |USING com.github.potix2.spark.google.spreadsheets
         |OPTIONS (worksheet "case2", spreadsheet "SpreadsheetSuite", serviceAccountId "$serviceAccountId", credentialPath "$testCredentialPath")
       """.stripMargin.replaceAll("\n", " "))

    assert(sqlContext.sql("SELECT id, firstname, lastname FROM SpreadsheetSuite").collect().size == 10)
  }
}
