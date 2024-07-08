package io.github.starofall.s3hypersync

import org.rogach.scallop.{ScallopConf, ScallopOption}

/** CLI parser and config */
class JobDefinition(arguments: Seq[String]) extends ScallopConf(arguments) {
  version("S3HyperSync.jar 0.1.5")
  noshort = true
  banner(
    """Usage: java -jar S3HyperSync.jar [OPTIONS]
      |A fast, efficient, cost-reducing, and memory-efficient S3 sync tool.
      |Options:
      |""".stripMargin)
  footer("\n")

  /** Number of workers */
  val numWorkers: ScallopOption[Int] = opt[Int](name = "workers", descr = "Number of workers", default = Some(64))

  /** Files larger than this size (in bytes) are uploaded using multipart */
  val putCutoffSize: ScallopOption[Int] = opt[Int](name = "put-cutoff-size", descr = "Files larger than this size (in bytes) are uploaded using multipart", default = Some(52428800))

  /** Size of each part in a multipart upload (in bytes) */
  val multipartSize: ScallopOption[Int] = opt[Int](name = "multipart-size", descr = "Size of each part in a multipart upload (in bytes)", default = Some(52428800))

  /** Sync mode: ALWAYS, IF_NOT_EXISTS, IF_SIZE_CHANGED */
  val syncMode: ScallopOption[String] = opt[String](name = "sync", descr = "Sync mode: ALWAYS, MISSING, CHANGED", validate = Set("ALWAYS", "MISSING", "CHANGED"), default = Some("MISSING"))

  /** Source S3 Key */
  val sourceKey: ScallopOption[String] = opt[String](name = "source-key", required = true, descr = "Source S3 Key")

  /** Source S3 Secret */
  val sourceSecret: ScallopOption[String] = opt[String](name = "source-secret", required = true, descr = "Source S3 Secret")

  /** Source S3 Region */
  val sourceRegion: ScallopOption[String] = opt[String](name = "source-region", required = true, descr = "Source S3 Region")

  /** Source S3 Bucket */
  val sourceBucket: ScallopOption[String] = opt[String](name = "source-bucket", required = true, descr = "Source S3 Bucket")

  /** Source S3 Prefix (must end with /) */
  val sourcePrefix: ScallopOption[String] = opt[String](name = "source-prefix", descr = "Source S3 Prefix (must end with /)", validate = _.endsWith("/"))

  /** Source S3 Endpoint */
  val sourceEndpoint: ScallopOption[String] = opt[String](name = "source-endpoint", descr = "Source S3 Endpoint")

  /** Use path style for source S3 */
  val sourcePathStyle: ScallopOption[Boolean] = opt[Boolean](name = "source-path-style", descr = "Use path style for source S3")

  /** Target S3 Key */
  val targetKey: ScallopOption[String] = opt[String](name = "target-key", required = true, descr = "Target S3 Key")

  /** Target S3 Secret */
  val targetSecret: ScallopOption[String] = opt[String](name = "target-secret", required = true, descr = "Target S3 Secret")

  /** Target S3 Region */
  val targetRegion: ScallopOption[String] = opt[String](name = "target-region", required = true, descr = "Target S3 Region")

  /** Target S3 Bucket */
  val targetBucket: ScallopOption[String] = opt[String](name = "target-bucket", required = true, descr = "Target S3 Bucket")

  /** Target S3 Prefix (must end with /) */
  val targetPrefix: ScallopOption[String] = opt[String](name = "target-prefix", descr = "Target S3 Prefix (must end with /)", validate = _.endsWith("/"))

  /** Target S3 Endpoint */
  val targetEndpoint: ScallopOption[String] = opt[String](name = "target-endpoint", descr = "Target S3 Endpoint")

  /** Use path style for target S3 */
  val targetPathStyle: ScallopOption[Boolean] = opt[Boolean](name = "target-path-style", descr = "Use path style for target S3")

  /** Storage tier: STANDARD, INTELLIGENT_TIERING, GLACIER_IR, GLACIER_IR_AUTO, DEEP_ARCHIVE, DEEP_ARCHIVE_AUTO */
  val storageTier: ScallopOption[String] = opt[String](name = "storage-tier", descr = "Storage tier: STANDARD, INTELLIGENT_TIERING, GLACIER_IR, GLACIER_IR_AUTO, DEEP_ARCHIVE, DEEP_ARCHIVE_AUTO", validate = Set("STANDARD", "INTELLIGENT_TIERING", "GLACIER_IR", "GLACIER_IR_AUTO", "DEEP_ARCHIVE", "DEEP_ARCHIVE_AUTO"), default = Some("STANDARD"))

  /** Verbose level (use multiple -v for increased verbosity) */
  val verbose: ScallopOption[Int] = tally(descr = "Verbose level (use multiple -v for increased verbosity)", short = 'v',noshort=false)

  /** Increase index speed if source prefix contains UUIDs */
  val uuidBoost: ScallopOption[Boolean] = opt[Boolean](name = "uuid-boost", descr = "Increase index speed if source prefix contains UUIDs", default = Some(false))

  /** Show what would be copied without actually copying */
  val dryRun: ScallopOption[Boolean] = opt[Boolean](name = "dry-run", descr = "Show what would be copied without actually copying", default = Some(false))

  /** Disable colored output */
  val noColor: ScallopOption[Boolean] = opt[Boolean](name = "no-color", descr = "Disable colored output", default = Some(false))

  /** Kill the process after N seconds */
  val timeout: ScallopOption[Int] = opt[Int](name = "timeout", descr = "Kills the process after N seconds")

  verify()
}
