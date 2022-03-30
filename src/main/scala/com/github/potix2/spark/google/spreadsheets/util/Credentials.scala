package com.github.potix2.spark.google.spreadsheets.util

import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.{GoogleCredentials, UserCredentials}

import java.time.Duration
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.io.Source

object Credentials {
  private val scopes = List(SheetsScopes.SPREADSHEETS)

  def credentialsFromFile(filename: String): HttpCredentialsAdapter = {
    val lines = Source.fromFile(filename)
    try {
      credentialsFromJsonString(lines.getLines().mkString)
    } finally {
      lines.close()
    }
  }

  def credentialsFromJsonString(oauth2JSON: String): HttpCredentialsAdapter = {
    val credentials: GoogleCredentials = GoogleCredentials.fromStream(
      new java.io.ByteArrayInputStream(oauth2JSON.getBytes(java.nio.charset.StandardCharsets.UTF_8))
    ).createScoped(scopes.asJava)

    credentials.refreshIfExpired()
    val accessToken = credentials.refreshAccessToken

    val oAuth2Credentials = GoogleCredentials.newBuilder()
      .setAccessToken(accessToken)
      .setRefreshMargin(Duration.ofDays(1))
      .build()
    new HttpCredentialsAdapter(oAuth2Credentials)
  }
}
