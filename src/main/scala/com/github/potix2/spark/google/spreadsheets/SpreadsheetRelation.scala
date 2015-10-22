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

  private lazy val rows: Seq[Map[String, String]] = {
    implicit val ctx = context
    val items = for {
      aSheet <- findSpreadsheet(spreadsheetName)
      aWorksheet <- aSheet.worksheets.find(w => w.entry.getTitle.getPlainText == worksheetName)
    } yield aWorksheet.rows

    if(items.isEmpty) {
      throw new RuntimeException(s"no such a spreadsheet: $spreadsheetName")
    }
    items.get
  }

  override def buildScan(): RDD[Row] = {
    val schema = inferSchema()
    sqlContext.sparkContext.makeRDD(rows).mapPartitions { iter =>
      iter.map { m =>
        Row.fromSeq(schema.fields.map(field => m(field.name)))
      }
    }
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = {
    throw new NotImplementedError()
  }

  private def inferSchema(): StructType =
    StructType(rows(0).keys.toList.map { fieldName =>
      StructField(fieldName, StringType, nullable = true)
    })

}
