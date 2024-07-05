package io.github.starofall.s3hypersync

import com.typesafe.config.ConfigFactory
import org.apache.pekko.stream.connectors.s3.S3Settings

import scala.jdk.CollectionConverters.MapHasAsJava

object SyncS3Settings {

  /** extracts the job definition source as s3 settings */
  def sourceConfig(d: JobDefinition): S3Settings = {
    buildS3Settings(d.sk.toOption.get,
                    d.ss.toOption.get,
                    d.sr.toOption.get,
                    d.se.toOption,
                    d.sp.toOption.getOrElse(false))
  }

  /** extracts the job definition target as s3 settings */
  def targetConfig(d: JobDefinition): S3Settings = {
    buildS3Settings(d.tk.toOption.get,
                    d.ts.toOption.get,
                    d.tr.toOption.get,
                    d.te.toOption,
                    d.tp.toOption.getOrElse(false))
  }

  /** creates a pekko config object */
  private def buildS3Settings(keyId:              String,
                              accessKey:          String,
                              region:             String,
                              endpointOverwrite:  Option[String],
                              usePathAccessStyle: Boolean): S3Settings = {
    val settingMap = scala.collection.mutable.Map(
      "buffer" -> "memory",
      "validate-object-key" -> "true",
      "retry-settings.max-retries" -> 6,
      "retry-settings.min-backoff" -> "200ms",
      "retry-settings.max-backoff" -> "10s",
      "retry-settings.random-factor" -> 0.0,
      "multipart-upload.retry-settings.max-retries" -> 6,
      "multipart-upload.retry-settings.min-backoff" -> "200ms",
      "multipart-upload.retry-settings.max-backoff" -> "10s",
      "multipart-upload.retry-settings.random-factor" -> 0.0,
      "sign-anonymous-requests" -> true,
      "access-style" -> "virtual",
      "aws.region.provider" -> "static",
      "aws.region.default-region" -> region,
      "aws.credentials.provider" -> "static",
      "aws.credentials.access-key-id" -> keyId,
      "aws.credentials.secret-access-key" -> accessKey
      )
    // allow setting a custom endpoint
    if (endpointOverwrite.isDefined) {
      settingMap.update("endpoint-url", endpointOverwrite.get)
    }
    // for legacy storage systems like minio, we can use path style
    if (usePathAccessStyle) {
      settingMap.update("access-style", "path")
    }
    S3Settings.create(ConfigFactory.parseMap(settingMap.asJava))
  }
}
