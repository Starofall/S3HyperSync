package io.github.starofall.s3hypersync

import io.github.starofall.s3hypersync.SyncLogging.Logger
import io.github.starofall.s3hypersync.SyncUtil._
import org.apache.pekko.actor.ActorSystem

import scala.concurrent.ExecutionContextExecutor


object MainApp extends Logger {

  /** main method for CLI interaction */
  def main(args: Array[String]): Unit = {
    val conf = new JobDefinition(args.toIndexedSeq)
    implicit val actorSystem: ActorSystem              = ActorSystem(
      "SyncSystem", buildConfig(conf))
    implicit val exc        : ExecutionContextExecutor = actorSystem.dispatcher
    SyncLogging.initLogger(conf)
    addTimeoutIfNeeded(conf)
    new SyncCommand(conf)
      .runSyncJob()
      .onComplete(handleFinalResult)
  }

}
