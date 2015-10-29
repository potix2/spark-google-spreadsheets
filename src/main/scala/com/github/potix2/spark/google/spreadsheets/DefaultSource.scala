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
import java.util.Calendar

import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SaveMode, SQLContext}
import org.apache.spark.sql.sources.{BaseRelation, CreatableRelationProvider, SchemaRelationProvider, RelationProvider}

class DefaultSource extends RelationProvider with SchemaRelationProvider with CreatableRelationProvider {
  final val DEFAULT_SPREADSHEET_NAME = "NewSparkSpreadsheet"
  final val DEFAULT_CREDENTIAL_PATH = "/etc/gdata/credential.p12"

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String]) = {
    createRelation(sqlContext, parameters, null)
  }

  val DEFAULT_SERVICE_ACCOUNT_ID = "/etc/gdata/credential.p12"
  private def defaultSpreadsheetName = DEFAULT_SPREADSHEET_NAME + Calendar.getInstance().getTime()

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String], schema: StructType) = {
    val serviceAccountId = parameters.getOrElse("serviceAccountId", sys.error("'serviceAccountId' must be specified for the google API account."))
    val spreadsheetName = parameters.getOrElse("spreadsheet", defaultSpreadsheetName)
    val worksheetName = parameters.getOrElse("worksheet", "NewWorksheet")
    val credentialPath = parameters.getOrElse("credentialPath", DEFAULT_CREDENTIAL_PATH)

    SpreadsheetRelation(
      SparkSpreadsheetService(serviceAccountId, new File(credentialPath)),
      spreadsheetName,
      worksheetName
    )(sqlContext)
  }

  override def createRelation(sqlContext: SQLContext, mode: SaveMode, parameters: Map[String, String], data: DataFrame): BaseRelation = null
}
