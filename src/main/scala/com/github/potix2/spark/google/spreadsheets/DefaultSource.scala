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

import com.github.potix2.spark.google.spreadsheets.util.Credentials
import org.apache.spark.sql.sources.{BaseRelation, CreatableRelationProvider, RelationProvider, SchemaRelationProvider}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}

class DefaultSource extends RelationProvider with SchemaRelationProvider with CreatableRelationProvider {

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String]): BaseRelation = {
    createRelation(sqlContext, parameters, null)
  }

  private[spreadsheets] def pathToSheetNames(path: String): (String, String) = {
    val elems = path.split('/')
    if (elems.length < 2)
      throw new Exception("'path' must be formed like '<spreadsheet>/<worksheet>'")

    (elems(0), elems(1))
  }

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String], schema: StructType) = {
    val path = parameters.getOrElse("path", sys.error("'path' must be specified for spreadsheets."))
    val (spreadsheetName, worksheetName) = pathToSheetNames(path)
    val context = createSpreadsheetContext(parameters)
    createRelation(sqlContext, context, spreadsheetName, worksheetName, schema)
  }


  override def createRelation(sqlContext: SQLContext, mode: SaveMode, parameters: Map[String, String], data: DataFrame): BaseRelation = {
    val path = parameters.getOrElse("path", sys.error("'path' must be specified for spreadsheets."))
    val (spreadsheetName, worksheetName) = pathToSheetNames(path)

    val context: SparkSpreadsheetService.SparkSpreadsheetContext = createSpreadsheetContext(parameters)

    val spreadsheet = SparkSpreadsheetService.findSpreadsheet(spreadsheetName)(context)
      .fold(throw new RuntimeException(s"no such a spreadsheet: $spreadsheetName"))(identity)

    spreadsheet.addWorksheet(worksheetName, data.schema, data.collect().toList, Util.toRowData)
    createRelation(sqlContext, context, spreadsheetName, worksheetName, data.schema)
  }

  private[spreadsheets] def createSpreadsheetContext(parameters: Map[String, String]): SparkSpreadsheetService.SparkSpreadsheetContext = {
    val credentials = (parameters.get("credentialsPath"), parameters.get("credentialsJson")) match {
      case (Some(credentialsPath), None) => Credentials.credentialsFromFile(credentialsPath)
      case (None, Some(credentialsJson)) => Credentials.credentialsFromJsonString(credentialsJson)
      case _ => throw new IllegalStateException("Either 'credentialsPath' or 'credentialsJson' parameter should be defined.")
    }
    SparkSpreadsheetService(credentials)
  }

  private[spreadsheets] def createRelation(sqlContext: SQLContext,
                                           context: SparkSpreadsheetService.SparkSpreadsheetContext,
                                           spreadsheetName: String,
                                           worksheetName: String,
                                           schema: StructType): SpreadsheetRelation =
    createRelation(sqlContext, context, spreadsheetName, worksheetName, Option(schema))

  private[spreadsheets] def createRelation(sqlContext: SQLContext,
                                           context: SparkSpreadsheetService.SparkSpreadsheetContext,
                                           spreadsheetName: String,
                                           worksheetName: String,
                                           schema: Option[StructType]
                                          ): SpreadsheetRelation =
    SpreadsheetRelation(context, spreadsheetName, worksheetName, schema)(sqlContext)
}
