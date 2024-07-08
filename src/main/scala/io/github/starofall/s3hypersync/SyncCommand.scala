package io.github.starofall.s3hypersync

import io.github.starofall.s3hypersync.PekkoFileSyncCompareStage.createSyncSource
import io.github.starofall.s3hypersync.SyncLogging.Logger
import io.github.starofall.s3hypersync.SyncModel.{FileSyncState, SyncStatus}
import io.github.starofall.s3hypersync.SyncUtil.retry
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Merge, Source}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class SyncCommand(conf: JobDefinition)
                 (implicit actorSystem: ActorSystem, exc: ExecutionContext)
  extends Logger {

  implicit val statistics: SyncStatistics = new SyncStatistics(conf)

  /** runs the main sync job */
  def runSyncJob(): Future[Unit] = {
    createSource()
      .wireTap(x=>statistics.statCall(x))
      .filter(syncFilter)
      .mapAsyncUnordered(conf.numWorkers())(x=>handleFileSync(x))
      .run()
      .map(_ => statistics.printFinalStatistics())
  }

  private def createSource() = {
    if (conf.uuidBoost.toOption.getOrElse(false)) {
      createUUIDBoosterSource()
    } else {
      createSyncSource(conf, None)
    }
  }

  /** if the prefix contains only UUIDs, we can just create 16 sources and merge them */
  private def createUUIDBoosterSource() = {
    assert(conf.sourcePrefix.isDefined, "UUID booster requires source prefix")
    assert(conf.targetPrefix.isDefined, "UUID booster requires target prefix")
    // if we know that the folder contains UUIDs,
    // we can active the iteration booster,
    // which will spawn multiple iteration calls for each
    // first character -> multiplexing the blocking calls for next1k
    // as we still compare the same prefix in the code itself,
    // the code should still work
    val extraPrefixedSources = "0123456789abcdef"
      .toCharArray.toList.map(c => createSyncSource(conf, Some(c.toString)))
    Source.combine(Source.empty, extraPrefixedSources.head, extraPrefixedSources.tail: _*)(Merge(_))
  }


  /** handles the sync of an individual file */
  private def handleFileSync(x: FileSyncState) = {
    retry(retries = 3, delay = 2.seconds) {
      if (conf.dryRun.getOrElse(false)) {
        log.info("[DRYRUN-COPY]".yellow + s" ${x.file.key.green} ${"->"} " +
                   s"${conf.targetPrefix.getOrElse("").magenta + x.file.relativeKey.cyan}")
        Future.successful(())
      } else {
        log.debug("[COPY-START] " + x.file.key + " -> "
                    + conf.targetPrefix.getOrElse("") + x.file.relativeKey)
        S3Connector.copyFile(
          conf,
          x.file.key,
          conf.targetPrefix.getOrElse("") + x.file.relativeKey,
          x.file.size).map { _ =>
          log.debug("[COPY-SUCCESS] " + x.file.key)
          statistics.incrementFilesCopied(x.file.size)
        }
      }
    }(actorSystem.dispatcher, actorSystem.scheduler)
  }


  /** A utility method to filter file synchronization based on the configuration job definitions */
  private def syncFilter(x: FileSyncState): Boolean = {
    conf.syncMode() match {
      case "ALWAYS"  => true // take all
      case "CHANGED" => x.status == SyncStatus.SizeChanged || x.status == SyncStatus.Missing
      case "MISSING" => x.status == SyncStatus.Missing
    }
  }
}
