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
