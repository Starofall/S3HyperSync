package io.github.starofall.s3hypersync

import io.github.starofall.s3hypersync.SyncLogging.Logger
import io.github.starofall.s3hypersync.SyncModel.SyncFile
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.connectors.s3._
import org.apache.pekko.stream.connectors.s3.scaladsl.S3
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.util.ByteString

import scala.concurrent.Future

object S3Connector extends Logger {

  /** copies a file from the source to the target bucket */
  def copyFile(job:      JobDefinition,
               sKey:     String,
               tKey:     String,
               fileSize: Long)
              (implicit actorSystem: ActorSystem,
               statistics: SyncStatistics): Future[Any] = {
    val s3Source: Source[ByteString, Future[ObjectMetadata]] = S3
      .getObject(job.sourceBucket.toOption.get, sKey)
      .withAttributes(S3Attributes.settings(SyncS3Settings.sourceConfig(job)))

    val storageString = {
      job.storageTier.getOrElse("STANDARD") match {
        case "STANDARD"            => "STANDARD"
        case "INTELLIGENT_TIERING" => "INTELLIGENT_TIERING"
        case "GLACIER_IA"          => "GLACIER_IA"
        case "GLACIER_IA_AUTO"     =>
          if (fileSize < 128 * 1024) {
            "STANDARD"
          } else {
            "GLACIER_IA"
          }
        case "DEEP_ARCHIVE"        => "DEEP_ARCHIVE"
        case "DEEP_ARCHIVE_AUTO"   =>
          if (fileSize < 128 * 1024) {
            "STANDARD"
          } else {
            "DEEP_ARCHIVE"
          }
        case _                     => throw new Exception("INVALID_STORAGE_TIER")
      }
    }

    if (fileSize < job.putCutoffSize.toOption.getOrElse(52428800)) {
      log.trace(s"[COPY-PUT] ${job.sourceBucket.toOption.get} / $sKey -> ${job.targetBucket.toOption.get} / $tKey")
      statistics.incrementAwsPutRequests(1)
      S3.putObject(job.targetBucket.toOption.get, tKey,
                   s3Headers = S3Headers().withCustomHeaders(Map("x-amz-storage-class" -> storageString)),
                   data = s3Source,
                   contentLength = fileSize)
        .withAttributes(S3Attributes.settings(SyncS3Settings.targetConfig(job)))
        .run()
    } else {
      log.trace(s"[COPY-MULTIPART] ${job.sourceBucket.toOption.get} / $sKey -> ${job.targetBucket.toOption.get} / $tKey")
      val multiPartChunkSize = job.multipartSize.getOrElse(52428800)
      statistics.incrementAwsPutRequests(2 + Math.max(1, (fileSize / multiPartChunkSize).toInt))
      val s3Sink: Sink[ByteString, Future[MultipartUploadResult]] = S3
        .multipartUploadWithHeaders(
          job.targetBucket.toOption.get, tKey,
          chunkSize = multiPartChunkSize,
          s3Headers = S3Headers().withCustomHeaders(Map("x-amz-storage-class" -> storageString)))
        .withAttributes(S3Attributes.settings(SyncS3Settings.targetConfig(job)))
      s3Source.runWith(s3Sink)
    }
  }

  def listBucket(bucket:     String,
                 prefix:     Option[String],
                 subPrefix:  Option[String], // an additional prefix that does not influence relative DIR
                 s3Settings: S3Settings): Source[SyncFile, NotUsed] = {
    val searchPrefix = (prefix, subPrefix) match {
      case (Some(x), Some(y)) => Some(x + y)
      case (None, Some(y))    => Some(y)
      case (Some(x), None)    => Some(x)
      case _                  => None
    }
    S3.listBucket(bucket, searchPrefix).withAttributes(S3Attributes.settings(s3Settings))
      .filterNot(x => x.size == 0 && x.key.endsWith("/")) // drop folders
      .map(x => SyncFile(
        x.bucketName, x.key, x.size,
        prefix match {
          case Some(value) => x.key.stripPrefix(value)
          case None        => x.key
        }))
  }

}
