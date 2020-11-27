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
package com.github.perbeatus.spark.google.spreadsheets

import com.github.perbeatus.spark.google.spreadsheets.SparkSpreadsheetService.SparkSpreadsheetContext
import com.github.perbeatus.spark.google.spreadsheets.util._
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

  import com.github.perbeatus.spark.google.spreadsheets.SparkSpreadsheetService._

  private val fieldMap = scala.collection.mutable.Map[String, String]()
  override def schema: StructType = userSchema.getOrElse(inferSchema())

  private lazy val aWorksheet: SparkWorksheet =
    findWorksheet(spreadsheetName, worksheetName)(context) match {
      case Right(aWorksheet) => aWorksheet
      case Left(e) => throw e
    }

  private lazy val rows: Seq[Map[String, String]] = aWorksheet.rows

  private[spreadsheets] def findWorksheet(spreadsheetName: String, worksheetName: String)(implicit ctx: SparkSpreadsheetContext): Either[Throwable, SparkWorksheet] =
    for {
      sheet <- findSpreadsheet(spreadsheetName).toRight(new RuntimeException(s"no such spreadsheet: $spreadsheetName")).right
      worksheet <- sheet.findWorksheet(worksheetName).toRight(new RuntimeException(s"no such worksheet: $worksheetName")).right
    } yield worksheet

  override def buildScan(): RDD[Row] = {
    val aSchema = schema
    val schemaMap = fieldMap.toMap
    sqlContext.sparkContext.makeRDD(rows).mapPartitions { iter =>
      iter.map { m =>
        var index = 0
        val rowArray = new Array[Any](aSchema.fields.length)
        while(index < aSchema.fields.length) {
          val field = aSchema.fields(index)
          rowArray(index) = if (m.contains(field.name)) {
            TypeCast.castTo(m(field.name), field.dataType, field.nullable)
          } else if (schemaMap.contains(field.name) && m.contains(schemaMap(field.name))) {
            TypeCast.castTo(m(schemaMap(field.name)), field.dataType, field.nullable)
          } else {
            null
          }
          index += 1
        }
        Row.fromSeq(rowArray)
      }
    }
  }

  override def insert(data: DataFrame, overwrite: Boolean): Unit = {
    if(!overwrite) {
      sys.error("Spreadsheet tables only support INSERT OVERWRITE for now.")
    }

    findWorksheet(spreadsheetName, worksheetName)(context) match {
      case Right(w) =>
        w.updateCells(data.schema, data.collect().toList, Util.toRowData)
      case Left(e) =>
        throw e
    }
  }

  def sanitizeColumnName(name: String): String =
  {
    name
      .replaceAll("[^a-zA-Z0-9]+", "_")    // Replace sequences of non-alphanumeric characters with underscores
      .replaceAll("_+$", "")               // Strip trailing underscores
      .replaceAll("^[0-9_]+", "")          // Strip leading underscores and digits
  }

  private def inferSchema(): StructType =
    StructType(aWorksheet.headers.toList.map { fieldName => {
      val sanitizedName = sanitizeColumnName(fieldName)
      fieldMap.put(sanitizedName, fieldName)
      StructField(sanitizedName, StringType, true)
    }})

}
