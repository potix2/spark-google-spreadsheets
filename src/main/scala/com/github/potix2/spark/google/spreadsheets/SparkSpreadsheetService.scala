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
import java.net.URL

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential.Builder
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.gdata.client.Query
import com.google.gdata.client.spreadsheet.{SpreadsheetQuery, SpreadsheetService}
import com.google.gdata.data.spreadsheet._
import com.google.gdata.data.{IEntry, IFeed, PlainTextConstruct}

import scala.collection.JavaConversions._


object SparkSpreadsheetService {
  private val SPREADSHEET_URL = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full")
  private val scopes = List("https://spreadsheets.google.com/feeds")
  private val APP_NAME = "spark-google-spreadsheets-1.0.0"

  case class SparkSpreadsheetContext(serviceAccountId: String, p12File: File) {
    private val service = new SpreadsheetService(APP_NAME)
    private val credential = authorize(serviceAccountId, p12File)

    private def authorize(serviceAccountId: String, p12File: File): GoogleCredential = {
      val credential = new Builder()
        .setTransport(GoogleNetHttpTransport.newTrustedTransport())
        .setJsonFactory(new JacksonFactory())
        .setServiceAccountId(serviceAccountId)
        .setServiceAccountPrivateKeyFromP12File(p12File)
        .setServiceAccountScopes(scopes)
        .build()

      credential.refreshToken()
      credential
    }

    def insert[E <: IEntry](feedUrl: URL, entry: E) = service.insert(feedUrl, entry)
    def getFeed[F <: IFeed](feedUrl: URL, feedClass: Class[F]) = service.getFeed(feedUrl, feedClass)
    def query[F <: IFeed](query: Query, feedClass: Class[F]) = service.query(query, feedClass)

    service.setOAuth2Credentials(credential)
  }

  case class SparkSpreadsheet(entry: SpreadsheetEntry) {
    def addWorksheet(sheetName: String, colNum: Int, rowNum: Int)(implicit context: SparkSpreadsheetContext): SparkWorksheet = {
      val worksheetEntry = new WorksheetEntry()
      worksheetEntry.setTitle(new PlainTextConstruct(sheetName))
      worksheetEntry.setColCount(colNum)
      worksheetEntry.setRowCount(rowNum)
      new SparkWorksheet(context.insert(entry.getWorksheetFeedUrl, worksheetEntry))
    }

    def findWorksheet(worksheetName: String)(implicit context: SparkSpreadsheetContext): Option[SparkWorksheet] =
      worksheets.find(_.entry.getTitle.getPlainText == worksheetName)

    private def worksheets(implicit context: SparkSpreadsheetContext): Seq[SparkWorksheet] =
      entry.getWorksheets.map(new SparkWorksheet(_))
  }

  case class SparkWorksheet(entry: WorksheetEntry) {
    def insertHeaderRow(headerColumnNames: List[String])(implicit context: SparkSpreadsheetContext) = {
      val cellFeed = context.getFeed(entry.getCellFeedUrl, classOf[CellFeed])
      headerColumnNames.zipWithIndex.foreach { case(colName, i) => cellFeed.insert(new CellEntry(1, i + 1, colName)) }
    }

    def insertRow(values: Map[String, Object])(implicit context: SparkSpreadsheetContext) = {
      val dataRow = new ListEntry()
      values.foreach { case(title, value) => dataRow.getCustomElements.setValueLocal(title, value.toString) }

      val listFeedUrl = entry.getListFeedUrl
      context.insert(listFeedUrl, dataRow)
    }

    def rows(implicit context: SparkSpreadsheetContext): Seq[Map[String, String]] =
      context.getFeed(entry.getListFeedUrl, classOf[ListFeed]).getEntries.toList.map { e =>
        val elems = e.getCustomElements
        elems.getTags.map(tag => (tag, elems.getValue(tag))).toMap
      }
  }

  /**
   * create new context of spareadsheets for spark
   * @param serviceAccountId
   * @param p12File
   * @return
   */
  def apply(serviceAccountId: String, p12File: File) = SparkSpreadsheetContext(serviceAccountId, p12File)

  /**
   * list of all spreadsheets
   * @param context
   * @return
   */
  def allSheets()(implicit context:SparkSpreadsheetContext): Seq[SparkSpreadsheet] =
    context.getFeed(SPREADSHEET_URL, classOf[SpreadsheetFeed]).getEntries.map(new SparkSpreadsheet(_))

  /**
   * find a sparedsheet by name
   * @param spreadsheetName
   * @param context
   * @return
   */
  def findSpreadsheet(spreadsheetName: String)(implicit context: SparkSpreadsheetContext): Option[SparkSpreadsheet] = {
    val query = new SpreadsheetQuery(SPREADSHEET_URL)
    query.setTitleQuery(spreadsheetName)
    context.query(query, classOf[SpreadsheetFeed]).getEntries.map(new SparkSpreadsheet(_)).headOption
  }
}
