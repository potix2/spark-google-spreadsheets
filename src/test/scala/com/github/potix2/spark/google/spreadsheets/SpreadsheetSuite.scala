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

  test("DSL test") {
    val results = sqlContext
    .sheet(
        serviceAccountId,
        testCredentialPath,
        "SpreadsheetSuite",
        "dsl_test")
    .select("col1")
    .collect()

    assert(results.size === 15)
  }
}
