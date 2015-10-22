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
package com.potix2.spark.google
import java.io.File

import org.apache.spark.sql.SQLContext

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
}