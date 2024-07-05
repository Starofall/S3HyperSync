package io.github.starofall.s3hypersync

import org.rogach.scallop.ScallopConf

/** CLI parser and config */
class JobDefinition(arguments: Seq[String]) extends ScallopConf(arguments) {
  version("S3HyperSync.jar 0.1.0")
  noshort = true
  banner(
    """Usage: java -jar s3-stream-sync.jar [OPTION]
      | A very fast, efficient, cost-reducing and memory efficient S3 Sync.
      |Options:
      |""".stripMargin)
  footer("\n")

  val workers       = opt[Int](descr = "Number of workers", validate = (x => x <= 64), default = Some(64))
  val putCutoffSize = opt[Int](descr = "Files bigger then X are uploaded using multipart",
                               default = Some(52428800))
  val multiPartSize = opt[Int](descr = "Size of a multipart upload",
                               default = Some(52428800))
  val sync          = opt[String](descr = "When to sync",
                                  validate = x => List("ALWAYS", "IF_NOT_EXISTS", "IF_SIZE_CHANGED").contains(x),
                                  default = Some("IF_NOT_EXISTS"))
  val sk            = opt[String](required = true, descr = "Source S3-Key")
  val ss            = opt[String](required = true, descr = "Source S3-Secret")
  val sr            = opt[String](required = true, descr = "Source S3-Region")
  val sb            = opt[String](required = true, descr = "Source S3-Bucket")
  val sx            = opt[String](validate = x => x.endsWith("/"), descr = "Source S3-Prefix")
  val se            = opt[String](descr = "Source S3-Endpoint")
  val sp            = opt[Boolean](descr = "Source S3 Use Path Style")
  val tk            = opt[String](required = true, descr = "Target S3-Key")
  val ts            = opt[String](required = true, descr = "Target S3-Secret")
  val tr            = opt[String](required = true, descr = "Target S3-Region")
  val tb            = opt[String](required = true, descr = "Target S3-Bucket")
  val tx            = opt[String](validate = x => x.endsWith("/"), descr = "Target S3-Prefix")
  val te            = opt[String](descr = "Target S3-Endpoint")
  val tp            = opt[Boolean](descr = "Target S3 Use Path Style")
  val tier          = opt[String](descr = "which storage tiere to use",
                                  validate = x => List("STANDARD", "INTELLIGENT_TIERING", "GLACIER_IR",
                                                       "GLACIER_IR_AUTO", "DEEP_ARCHIVE", "DEEP_ARCHIVE_AUTO").contains(x),
                                  default = Some("STANDARD"))
  val v             = tally(descr = "Verbose Level (use multiple v)", short = 'v',noshort=false)
  val uuidbooster   = opt[Boolean](default = Some(false),
                                   descr = "If the source prefix contains UUIDs, this 16x the index speed")
  val dryrun        = opt[Boolean](default = Some(false),
                                   descr = "Shows what it would copy")
  val nocolor        = opt[Boolean](default = Some(false),
                                   descr = "Dont print out in color")
  verify()

  lazy val sourceS3Config = SyncS3Settings.sourceConfig(this)
  lazy val targetS3Config = SyncS3Settings.targetConfig(this)

}
// @formatter:on
