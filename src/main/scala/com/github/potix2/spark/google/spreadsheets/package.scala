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
package com.github.potix2.spark.google

import java.io.File

import com.github.potix2.spark.google.spreadsheets.SparkSpreadsheetService.{SparkWorksheet, SparkSpreadsheet}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Row, SQLContext}

package object spreadsheets {
  /**
   * Add a method, `sheet`, to SQLContext that allows reading Google Spreadsheets data.
   * @param sqlContext
   */
  implicit class SpreadsheetContext(sqlContext: SQLContext) extends Serializable {
    def sheet(
               serviceAccountId: String,
               credentialPath: String,
               spreadsheetName: String,
               worksheetName: String) = {
      val context = SparkSpreadsheetService(serviceAccountId, new File(credentialPath))
      val sheetRelation = SpreadsheetRelation(
        context,
        spreadsheetName,
        worksheetName
      )(sqlContext)
      sqlContext.baseRelationToDataFrame(sheetRelation)
    }
  }

  /**
   * Saves DataFrame as google spreadsheets.
   */
  implicit class SpreadsheetDataFrame(dataFrame: DataFrame) {
    private[spreadsheets] def convert(schema: StructType, row: Row): Map[String, Object] =
      schema.iterator.zipWithIndex.map { case (f, i) => f.name -> row(i).asInstanceOf[AnyRef]} toMap

    private[spreadsheets] def createWorksheet(spreadsheet: SparkSpreadsheet, name: String)(implicit context:SparkSpreadsheetService.SparkSpreadsheetContext): SparkWorksheet = {
      val columns = dataFrame.schema.fieldNames
      val worksheet = spreadsheet.addWorksheet(name, columns.length, dataFrame.count().toInt)
      worksheet.insertHeaderRow(columns)

      worksheet
    }

    def saveAsSheet(sheetName: String, parameters: Map[String, String] = Map()): Unit = {
      val serviceAccountId = parameters("serviceAccountId")
      val credentialPath = parameters("credentialPath")
      val worksheetName = parameters("worksheetName")

      implicit val context = SparkSpreadsheetService(serviceAccountId, new File(credentialPath))
      val spreadsheet = SparkSpreadsheetService.findSpreadsheet(sheetName)
      if(!spreadsheet.isDefined)
        throw new RuntimeException(s"no such a spreadsheet: $sheetName")

      val worksheet = createWorksheet(spreadsheet.get, worksheetName)
      dataFrame.collect().foreach(row => worksheet.insertRow(convert(dataFrame.schema, row)))
    }
  }
}