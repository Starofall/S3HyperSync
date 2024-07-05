package io.github.starofall.s3hypersync

import io.github.starofall.s3hypersync.SyncLogUtil.Logger
import io.github.starofall.s3hypersync.SyncModel.{FileSyncState, SyncStatus}
import org.apache.pekko.actor.{ActorSystem, Cancellable}

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object SyncStatistics extends Logger {
  private var filesScanned                           = new AtomicInteger(0)
  private var filesMissing                           = new AtomicInteger(0)
  private var filesChanged                           = new AtomicInteger(0)
  private var filesCopied                            = new AtomicInteger(0)
  private var filesExisting                          = new AtomicInteger(0)
  private var filesScannedLastSecond                 = new AtomicInteger(0)
  private var awsPutRequests                         = new AtomicInteger(0)
  private var bytesTransferredLastSecond: AtomicLong = new AtomicLong(0L)
  private var totalBytesTransferred     : AtomicLong = new AtomicLong(0L)
  private var lastUpdateTime            : Long       = System.currentTimeMillis()
  private var started                                = false


  def statCall(conf: JobDefinition, x: FileSyncState): Unit = {
    SyncStatistics.setStarted()
    x.status match {
      case SyncStatus.Missing     => SyncStatistics.incrementFilesMissing()
      case SyncStatus.SizeChanged => SyncStatistics.incrementFilesChanged()
      case SyncStatus.Exists      => SyncStatistics.incrementFilesExisting()
    }
    SyncStatistics.incrementFilesScanned()
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

  def incrementFilesCopied(): Unit = {
    filesCopied.incrementAndGet()
  }

  def addBytesTransferred(size: Long): Unit = {
    bytesTransferredLastSecond.addAndGet(size)
    totalBytesTransferred.addAndGet(size)
  }

  def incrementAwsPutRequests(l: Int) = awsPutRequests.addAndGet(l)

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

  def initStatistics(conf: JobDefinition)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext): Cancellable = {
    log.info(s"[INIT] ".yellow +
               s"${conf.sb.toOption.get}/${conf.sx.getOrElse("")} ".green +
               s"-> " +
               s"${conf.tb.toOption.get}/${conf.tx.getOrElse("")}".cyan)
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
                 f"Bandwidth: $MBspeed%.2f MB/s, ".magenta +
                 f"Files $speed%.2f files/sec, ".red +
                 f"Files scanned: $filesScanned".cyan)
    }
  }
}
