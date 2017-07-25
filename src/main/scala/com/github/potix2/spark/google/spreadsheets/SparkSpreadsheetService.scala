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

import java.net.URL
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.model._
import com.google.api.services.sheets.v4.{Sheets, SheetsScopes}
import org.apache.spark.sql.types.StructType

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


object SparkSpreadsheetService {
  private val SPREADSHEET_URL = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full")
  private val scopes = List(SheetsScopes.SPREADSHEETS)
  private val APP_NAME = "spark-google-spreadsheets-1.0.0"
  private val HTTP_TRANSPORT: NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()
  private val JSON_FACTORY: JacksonFactory = JacksonFactory.getDefaultInstance()

  case class SparkSpreadsheetContext(serviceAccountId: String, client_json: String) {

    private val credential = authorize(serviceAccountId, client_json)
    lazy val service =
      new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
        .setApplicationName(APP_NAME)
        .build()


    private def authorize(serviceAccountId: String, client_json: String): GoogleCredential = {

      // Reading credential json string
      val in :InputStream = new ByteArrayInputStream(client_json.getBytes(StandardCharsets.UTF_8))
      val credential = GoogleCredential.fromStream(in, HTTP_TRANSPORT,JSON_FACTORY).createScoped(scopes)

      credential.refreshToken()
      credential
    }

    def findSpreadsheet(spreadSheetId: String): SparkSpreadsheet = {
      SparkSpreadsheet(this, service.spreadsheets().get(spreadSheetId).execute())
    }

    def query(spreadsheetId: String, range: String): ValueRange =
      service.spreadsheets().values().get(spreadsheetId, range).execute()
  }

  case class SparkSpreadsheet(context:SparkSpreadsheetContext, private var spreadsheet: Spreadsheet) {
    def name: String = spreadsheet.getProperties.getTitle
    def getWorksheets: Seq[SparkWorksheet] =
      spreadsheet.getSheets.map(new SparkWorksheet(context, spreadsheet, _))

    def nextSheetId: Integer =
      getWorksheets.map(_.sheet.getProperties.getSheetId).max + 1

    def addWorksheet(sheetName: String, colNum: Integer, rowNum: Integer): Unit = {
      val addSheetRequest = new AddSheetRequest()
      addSheetRequest.setProperties(
        new SheetProperties()
          .setSheetId(nextSheetId)
          .setTitle(sheetName)
          .setGridProperties(
            new GridProperties()
              .setColumnCount(colNum)
              .setRowCount(rowNum)))

      val requests = List(
        new Request().setAddSheet(addSheetRequest)
      )

      context.service.spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId,
        new BatchUpdateSpreadsheetRequest()
          .setRequests(requests)).execute()

      spreadsheet = context.service.spreadsheets().get(spreadsheet.getSpreadsheetId).execute()
    }

    def addWorksheet[T](sheetName: String, schema: StructType, data: List[T], extractor: T => RowData): Unit = {
      val colNum = schema.fields.size
      val rowNum = data.size + 1
      val nextId = nextSheetId

      val addSheetRequest = new AddSheetRequest()
      addSheetRequest.setProperties(
        new SheetProperties()
          .setSheetId(nextId)
          .setTitle(sheetName)
          .setGridProperties(
            new GridProperties()
              .setColumnCount(colNum)
              .setRowCount(rowNum)))

      val headerValues: List[CellData] = schema.fields.map { field =>
        new CellData()
          .setUserEnteredValue(new ExtendedValue()
            .setStringValue(field.name))
      }.toList

      val updateHeaderRequest = new UpdateCellsRequest()
        .setStart(new GridCoordinate()
          .setSheetId(nextId)
          .setRowIndex(0)
          .setColumnIndex(0))
        .setRows(List(new RowData().setValues(headerValues)))
        .setFields("userEnteredValue")

      val updateRowsRequest = new UpdateCellsRequest()
        .setStart(new GridCoordinate()
          .setSheetId(nextId)
          .setRowIndex(1)
          .setColumnIndex(0))
        .setRows(data.map(extractor))
        .setFields("userEnteredValue")

      val requests = List(
        new Request().setAddSheet(addSheetRequest),
        new Request().setUpdateCells(updateHeaderRequest),
        new Request().setUpdateCells(updateRowsRequest)
      )

      context.service.spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId,
        new BatchUpdateSpreadsheetRequest()
          .setRequests(requests)).execute()

      spreadsheet = context.service.spreadsheets().get(spreadsheet.getSpreadsheetId).execute()
    }

    def findWorksheet(worksheetName: String): Option[SparkWorksheet] = {
      val worksheets: Seq[SparkWorksheet] = getWorksheets
      worksheets.find(_.sheet.getProperties.getTitle == worksheetName)
    }

    def deleteWorksheet(worksheetName: String): Unit = {
      val worksheet: Option[SparkWorksheet] = findWorksheet(worksheetName)
      if(worksheet.isDefined) {
        val request = new Request()
        val sheetId = worksheet.get.sheet.getProperties.getSheetId
        request.setDeleteSheet(new DeleteSheetRequest()
          .setSheetId(sheetId))

        context.service.spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId,
          new BatchUpdateSpreadsheetRequest().setRequests(List(request).asJava)).execute()

        spreadsheet = context.service.spreadsheets().get(spreadsheet.getSpreadsheetId).execute()
      }
    }
  }

  case class SparkWorksheet(context: SparkSpreadsheetContext, spreadsheet: Spreadsheet, sheet: Sheet) {
    def name: String = sheet.getProperties.getTitle
    lazy val values = {
      val valueRange = context.query(spreadsheet.getSpreadsheetId, name)
      if ( valueRange.getValues != null )
        valueRange.getValues
      else
        List[java.util.List[Object]]().asJava
    }

    lazy val headers =
      values.headOption.map { row => row.map(_.toString) }.getOrElse(List())

    def updateCells[T](schema: StructType, data: List[T], extractor: T => RowData): Unit = {
      val colNum = schema.fields.size
      val rowNum = data.size + 2
      val sheetId = sheet.getProperties.getSheetId

      val updatePropertiesRequest = new UpdateSheetPropertiesRequest()
      updatePropertiesRequest.setProperties(
        new SheetProperties()
          .setSheetId(sheetId)
          .setGridProperties(
            new GridProperties()
              .setColumnCount(colNum)
              .setRowCount(rowNum)))
        .setFields("gridProperties(rowCount,columnCount)")

      val headerValues: List[CellData] = schema.fields.map { field =>
        new CellData()
          .setUserEnteredValue(new ExtendedValue()
            .setStringValue(field.name))
      }.toList

      val updateHeaderRequest = new UpdateCellsRequest()
        .setStart(new GridCoordinate()
          .setSheetId(sheetId)
          .setRowIndex(0)
          .setColumnIndex(0))
        .setRows(List(new RowData().setValues(headerValues)))
        .setFields("userEnteredValue")

      val updateRowsRequest = new UpdateCellsRequest()
        .setStart(new GridCoordinate()
          .setSheetId(sheetId)
          .setRowIndex(1)
          .setColumnIndex(0))
        .setRows(data.map(extractor))
        .setFields("userEnteredValue")

      val requests = List(
        new Request().setUpdateSheetProperties(updatePropertiesRequest),
        new Request().setUpdateCells(updateHeaderRequest),
        new Request().setUpdateCells(updateRowsRequest)
      )

      context.service.spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId,
        new BatchUpdateSpreadsheetRequest()
          .setRequests(requests)).execute()
    }

    def rows: Seq[Map[String, String]] =
      if(values.isEmpty) {
        Seq()
      }
      else {
        values.tail.map { row => headers.zip(row.map(_.toString)).toMap }
      }
  }

  /**
   * create new context of spareadsheets for spark
    *
    * @param serviceAccountId
   * @param client_json
   * @return
   */
  def apply(serviceAccountId: String, client_json: String) = SparkSpreadsheetContext(serviceAccountId, client_json)

  /**
   * find a spreadsheet by name
    *
    * @param spreadsheetName
   * @param context
   * @return
   */
  def findSpreadsheet(spreadsheetName: String)(implicit context: SparkSpreadsheetContext): Option[SparkSpreadsheet] =
    Some(context.findSpreadsheet(spreadsheetName))
}
