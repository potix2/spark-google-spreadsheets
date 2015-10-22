package com.github.potix2.spark.google.spreadsheets

import com.github.potix2.spark.google.spreadsheets.SparkSpreadsheetService.SparkSpreadsheetContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.sources.{BaseRelation, InsertableRelation, TableScan}
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SQLContext}

case class SpreadsheetRelation protected[spark] (
                                                  context:SparkSpreadsheetContext,
                                                  spreadsheetName: String,
                                                  worksheetName: String)(@transient val sqlContext: SQLContext)
  extends BaseRelation with TableScan with InsertableRelation {
  import com.github.potix2.spark.google.spreadsheets.SparkSpreadsheetService._

  override def schema: StructType = inferSchema()

  override def buildScan(): RDD[Row] = {
    implicit val ctx = context
    val schema = inferSchema()
    val schemaFields = schema.fields
    val rows = for {
      aSheet <- findSpreadsheet(spreadsheetName)
      aWorksheet <- aSheet.worksheets.find(w => w.entry.getTitle.getPlainText == worksheetName)
    } yield aWorksheet.rows

    if(rows.isEmpty) {
      throw new RuntimeException(s"no such a spreadsheet: $spreadsheetName")
    }

    sqlContext.sparkContext.makeRDD(rows.get).mapPartitions { iter =>
      iter.map { m =>
        Row.fromSeq(schemaFields.map(field => m(field.name)))
      }
    }
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = {
    throw new NotImplementedError()
  }

  private def inferSchema(): StructType =
    StructType(List(
      StructField("col1", StringType, nullable = true),
      StructField("col2", StringType, nullable = true),
      StructField("col3", StringType, nullable = true)
    ))
}
