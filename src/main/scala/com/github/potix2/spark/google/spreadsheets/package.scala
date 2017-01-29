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

import org.apache.spark.sql._

package object spreadsheets {
  /**
   * Add a method, `spreadsheet`, to DataFrameReader that allows reading Google Spreadsheets data.
   */
  implicit class SpreadsheetDataFrameReader(reader: DataFrameReader) {
    def spreadsheet: String => DataFrame =
      reader.format("com.github.potix2.spark.google.spreadsheets").load
  }

  /**
   * Add a method, `spreadsheet`, to DataFrameWriter that allows you to write Google Spreadsheets data.
   */
  implicit class SpreadsheetDataFrameWriter[T](writer: DataFrameWriter[T]) {
    def spreadsheet: String => Unit =
      writer.format("com.github.potix2.spark.google.spreadsheets").save
  }
}