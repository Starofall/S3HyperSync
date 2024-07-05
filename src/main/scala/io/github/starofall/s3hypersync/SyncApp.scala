package io.github.starofall.s3hypersync

import io.github.starofall.s3hypersync.SyncLogUtil.Logger
import io.github.starofall.s3hypersync.SyncModel.{FileSyncState, SyncStatus}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.{Merge, Source}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}


object SyncApp extends Logger {
  implicit val actorSystem: ActorSystem              = ActorSystem("SyncSystem")
  implicit val exc        : ExecutionContextExecutor = actorSystem.dispatcher

  def main(args: Array[String]): Unit = {
    val conf = new JobDefinition(args.toIndexedSeq)
    SyncLogUtil.colorActive = !conf.nocolor.getOrElse(false)
    SyncLogUtil.setRootLogLevel(conf.v.getOrElse(0))
    SyncStatistics.initStatistics(conf)
    createSource(conf)
      .wireTap(x => SyncStatistics.statCall(conf, x))
      .filter(x => syncFilter(conf, x))
      .mapAsyncUnordered(conf.workers.getOrElse(64)) { x =>
        if (conf.dryrun.getOrElse(false)) {
          log.info("[DRYRUN-COPY]".yellow + s" ${x.file.key.green} ${"->"} ${conf.tx.toOption.getOrElse("").magenta + x.file.relativeKey.cyan}")
          Future.successful(())
        } else {
          log.debug("[COPY-START] " + x.file.key + " -> " + conf.tx.toOption.getOrElse("") + x.file.relativeKey)
          S3Connector.copyFile(conf,
                               x.file.key,
                               conf.tx.toOption.getOrElse("") + x.file.relativeKey,
                               x.file.size).map { _ =>
            log.debug("[COPY-SUCCESS] " + x.file.key)
            SyncStatistics.incrementFilesCopied()
            SyncStatistics.addBytesTransferred(x.file.size) // Track bytes transferred
          }
        }
      }
      .run()
      .onComplete(handleFinalResult)
  }

  private def handleFinalResult(result: Try[_]) = {
    result match {
      case Failure(exception) =>
        log.error("Error Running Sync")
        exception.printStackTrace(System.err)
        actorSystem.registerOnTermination(() => {
          System.exit(1)
        })
        actorSystem.terminate()
      case Success(_)         =>
        SyncStatistics.printFinalStatistics()
        actorSystem.registerOnTermination(() => {
          System.exit(0)
        })
        actorSystem.terminate()
    }

  }



  private def createSource(conf: JobDefinition) = {
    if (conf.uuidbooster.toOption.getOrElse(false)) {
      createUUIDBoosterSource(conf)
    } else {
      createSyncSource(conf, None)
    }
  }

  /** if the prefix contains only UUIDs, we can just create 16 sources and merge them */
  private def createUUIDBoosterSource(conf: JobDefinition) = {
    assert(conf.sx.isDefined, "UUID booster requires source prefix")
    assert(conf.tx.isDefined, "UUID booster requires target prefix")
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

  private def createSyncSource(conf: JobDefinition, additionalPrefix: Option[String]) = {
    PekkoFileSyncCompareStage
      .compareFilesToTarget(
        S3Connector.listBucket(conf.sb.toOption.get,
                               conf.sx.toOption,
                               additionalPrefix,
                               conf.sourceS3Config)
                   .buffer(10000, OverflowStrategy.backpressure).async,
        S3Connector.listBucket(conf.tb.toOption.get,
                               conf.tx.toOption,
                               additionalPrefix,
                               conf.targetS3Config)
                   .buffer(10000, OverflowStrategy.backpressure).async)
  }

  private def syncFilter(conf: JobDefinition, x: FileSyncState) = {
    conf.sync.toOption.getOrElse("IF_NOT_EXISTS") match {
      case "ALWAYS"          =>
        true // take all
      case "IF_SIZE_CHANGED" =>
        x.status == SyncStatus.SizeChanged || x.status == SyncStatus.Missing
      case "IF_NOT_EXISTS"   =>
        x.status == SyncStatus.Missing
    }
  }


}
