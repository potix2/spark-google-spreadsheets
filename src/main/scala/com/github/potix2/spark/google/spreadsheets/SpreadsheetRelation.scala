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
                                                  worksheetName: String,
                                                  userSchema: Option[StructType] = None)(@transient val sqlContext: SQLContext)
  extends BaseRelation with TableScan with InsertableRelation {

  import com.github.potix2.spark.google.spreadsheets.SparkSpreadsheetService._

  override def schema: StructType = userSchema.getOrElse(inferSchema())

  private lazy val rows: Seq[Map[String, String]] =
    findWorksheet(spreadsheetName, worksheetName)(context) match {
      case Right(aWorksheet) => aWorksheet.rows(context)
      case Left(e) => throw e
    }

  private[spreadsheets] def findWorksheet(spreadsheetName: String, worksheetName: String)(implicit ctx: SparkSpreadsheetContext): Either[Throwable, SparkWorksheet] =
    for {
      sheet <- findSpreadsheet(spreadsheetName).toRight(new RuntimeException(s"no such a worksheet: $worksheetName")).right
      worksheet <- sheet.findWorksheet(worksheetName).toRight(new RuntimeException(s"no such a spreadsheet: $spreadsheetName")).right
    } yield worksheet

  override def buildScan(): RDD[Row] = {
    val aSchema = schema
    sqlContext.sparkContext.makeRDD(rows).mapPartitions { iter =>
      iter.map { m =>
        Row.fromSeq(aSchema.fields.map(field => m(field.name)))
      }
    }
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = {
    if(!overwrite) {
      sys.error("Spreadsheet tables only support INSERT OVERWRITE for now.")
    }

    val columns = data.schema.fieldNames
    findWorksheet(spreadsheetName, worksheetName)(context) match {
      case Right(w) => {
        w.insertHeaderRow(columns)(context)
        data.collect().foreach(row => w.insertRow(Util.convert(data.schema, row))(context))
      }
      case Left(e) => throw e
    }
  }

  private def inferSchema(): StructType =
    StructType(rows(0).keys.toList.map { fieldName =>
      StructField(fieldName, StringType, nullable = true)
    })

}
