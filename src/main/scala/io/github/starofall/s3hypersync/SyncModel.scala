package io.github.starofall.s3hypersync

object SyncModel {

  /** Represents the status of sync */
  trait SyncStatus

  /**
   * Describes the sync status of a given file
   *
   * @param status status
   * @param file   ref to syncFile
   */
  case class FileSyncState(status: SyncStatus,
                           file:   SyncFile)

  /**
   * references a file on s3
   *
   * @param bucket      the bucket this file exists on
   * @param key         the full s3 key
   * @param size        the byteSize of the file
   * @param relativeKey the key relative to the root dir of the job
   */
  case class SyncFile(bucket:      String,
                      key:         String,
                      size:        Long,
                      relativeKey: String)

  /** Object contains statuses of sync */
  object SyncStatus {

    /** Object exists in S3 */
    case object Exists extends SyncStatus

    /** Size of the object has changed in S3 */
    case object SizeChanged extends SyncStatus

    /** Object is missing in S3 */
    case object Missing extends SyncStatus

  }

}
