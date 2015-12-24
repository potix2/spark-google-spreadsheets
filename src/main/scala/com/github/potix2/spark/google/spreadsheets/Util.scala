package com.github.potix2.spark.google.spreadsheets

import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType

object Util {
  def convert(schema: StructType, row: Row): Map[String, Object] =
    schema.iterator.zipWithIndex.map { case (f, i) => f.name -> row(i).asInstanceOf[AnyRef]} toMap
}
