package io.github.starofall.s3hypersync

import com.typesafe.config.ConfigFactory
import org.apache.pekko.stream.connectors.s3.S3Settings

import java.nio.file.Files
import scala.jdk.CollectionConverters.MapHasAsJava

object SyncS3Settings {

  /** extracts the job definition source as s3 settings */
  def sourceConfig(d: JobDefinition): S3Settings = {
    buildS3Settings(d.sourceKey.toOption.get,
                    d.sourceSecret.toOption.get,
                    d.sourceRegion.toOption.get,
                    d.sourceEndpoint.toOption,
                    d.sourcePathStyle.toOption.getOrElse(false))
  }

  /** extracts the job definition target as s3 settings */
  def targetConfig(d:          JobDefinition,
                   isHugeFile: Boolean = false): S3Settings = {
    buildS3Settings(d.targetKey.toOption.get,
                    d.targetSecret.toOption.get,
                    d.targetRegion.toOption.get,
                    d.targetEndpoint.toOption,
                    d.targetPathStyle.toOption.getOrElse(false),
                    isHugeFile)
  }

  /** creates a pekko config object */
  private def buildS3Settings(keyId:              String,
                              accessKey:          String,
                              region:             String,
                              endpointOverwrite:  Option[String],
                              usePathAccessStyle: Boolean,
                              isHugeFile:         Boolean = false): S3Settings = {
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
    // on huge files we use file buffering (else we might run OOM)
    if (isHugeFile) {
      settingMap.update("buffer", "disk")
      settingMap.update("disk-buffer-path", Files.createTempDirectory("s3hypersync").toAbsolutePath.toString)
    }
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
