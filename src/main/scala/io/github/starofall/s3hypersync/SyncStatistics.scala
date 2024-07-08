package io.github.starofall.s3hypersync

import io.github.starofall.s3hypersync.SyncLogging.Logger
import io.github.starofall.s3hypersync.SyncModel.{FileSyncState, SyncStatus}
import org.apache.pekko.actor.{ActorSystem, Cancellable}

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class SyncStatistics(conf: JobDefinition)
                    (implicit actorSystem: ActorSystem, executionContext: ExecutionContext)
  extends Logger {

  val filesScanned                           = new AtomicInteger(0)
  val filesMissing                           = new AtomicInteger(0)
  val filesChanged                           = new AtomicInteger(0)
  val filesCopied                            = new AtomicInteger(0)
  val filesExisting                          = new AtomicInteger(0)
  val filesScannedLastSecond                 = new AtomicInteger(0)
  val awsPutRequests                         = new AtomicInteger(0)
  val bytesTransferredLastSecond: AtomicLong = new AtomicLong(0L)
  val totalBytesTransferred     : AtomicLong = new AtomicLong(0L)
  var lastUpdateTime            : Long       = System.currentTimeMillis()
  var started                                = false

  initStatistics()

  def statCall(x: FileSyncState): Unit = {
    setStarted()
    x.status match {
      case SyncStatus.Missing     => incrementFilesMissing()
      case SyncStatus.SizeChanged => incrementFilesChanged()
      case SyncStatus.Exists      => incrementFilesExisting()
    }
    incrementFilesScanned()
    log.trace(x.status.toString + "->" + x.file.key)
  }

  def incrementFilesScanned(): Unit = {
    filesScanned.incrementAndGet()
    filesScannedLastSecond.incrementAndGet()
  }

  def incrementFilesMissing(): Unit = filesMissing.incrementAndGet()

  def incrementFilesChanged(): Unit = filesChanged.incrementAndGet()

  def incrementFilesExisting(): Unit = filesExisting.incrementAndGet()

  def setStarted(): Unit = started = true

  def incrementFilesCopied(size: Long): Unit = {
    filesCopied.incrementAndGet()
    bytesTransferredLastSecond.addAndGet(size)
    totalBytesTransferred.addAndGet(size)
  }

  def incrementAwsPutRequests(l: Int): Int = awsPutRequests.addAndGet(l)

  def printFinalStatistics(): Unit = {
    log.info("##############")
    log.info("## Sync Stats")
    log.info(s"# Missing | ${filesMissing.get().toString.yellow}")
    log.info(s"# Changed | ${filesChanged.get().toString.yellow}")
    log.info(s"# Exists  | ${filesExisting.get().toString.yellow}")
    log.info("##############")
    log.info("## Copy Stats")
    log.info(s"# Files   | ${filesCopied.get().toString.yellow}")
    log.info(s"# MB      | ${(totalBytesTransferred.get() / 1024.0 / 1024.0).round.toString.yellow}")
    log.info("##############")
    log.info("## Cost Stats")
    log.info(s"# Puts    | ${awsPutRequests.get().toString.yellow}")
    log.info("##############")
  }

  def initStatistics(): Cancellable = {
    log.info(s"[INIT] ".yellow +
               s"${conf.sourceBucket.toOption.get}/${conf.sourcePrefix.getOrElse("")} ".green +
               s"-> " +
               s"${conf.targetBucket.toOption.get}/${conf.targetPrefix.getOrElse("")}".cyan)
    // Schedule a task to print statistics every second
    actorSystem.scheduler.scheduleAtFixedRate(1.second, 5.second) {
      () => printStatistics()
    }
  }

  def printStatistics(): Unit = {
    val currentTime = System.currentTimeMillis()
    val duration    = (currentTime - lastUpdateTime) / 1000.0
    val speed       = filesScannedLastSecond.get() / duration
    val MBspeed     = bytesTransferredLastSecond.get() / (1024.0 * 1024.0 * duration) // in MB/s
    lastUpdateTime = currentTime
    filesScannedLastSecond.set(0)
    bytesTransferredLastSecond.set(0) // Reset for the next interval
    if (started) {
      log.info(f"[STATS] ".yellow +
                 f"Bandwidth: $MBspeed%.2f MB/s | ".magenta +
                 f"Files: $speed%.2f f/sec".red +
                 f" | Scanned: $filesScanned".cyan +
                 f" | Copied: $filesCopied".cyan +
                 f" | Missing: $filesMissing".cyan +
                 f" | Changed: $filesChanged".cyan +
                 f" | Existing: $filesExisting".cyan)
    }
  }
}
