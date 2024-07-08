package io.github.starofall.s3hypersync

import com.robothy.s3.rest.LocalS3
import com.robothy.s3.rest.bootstrap.LocalS3Mode
import org.apache.pekko.actor.ActorSystem
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}

class SyncCommandTest extends AnyFunSuiteLike {

  implicit val actorSystem: ActorSystem              = ActorSystem("TestSyncSystem")
  implicit val exc        : ExecutionContextExecutor = actorSystem.dispatcher

  def createConfig(dryRun: Boolean): JobDefinition = {
    new JobDefinition(List(
      "--source-key", "DUMMY",
      "--source-secret", "DUMMY",
      "--source-region", "region",
      "--source-path-style",
      "--source-endpoint", "http://localhost:19090",
      "--target-key", "DUMMY2",
      "--target-secret", "DUMMY2",
      "--target-region", "region",
      "--target-endpoint", "http://localhost:19090",
      "--target-path-style",
      "--source-bucket", "bucket-a",
      "--target-bucket", "bucket-b") ++ (if (dryRun) List("--dry-run") else List()))
  }


  test("simple sync") {
    val localS3 = LocalS3.builder
                         .mode(LocalS3Mode.IN_MEMORY)
                         .dataPath("./tests/simple")
                         .port(19090).build
    localS3.start()

    val dryRunConfig = createConfig(dryRun = true)
    SyncLogging.initLogger(dryRunConfig)
    val dryRunCommand = new SyncCommand(dryRunConfig)
    dryRunCommand.statistics.filesScanned.get() mustBe 0
    Await.result(dryRunCommand.runSyncJob(), 30.seconds)
    dryRunCommand.statistics.filesScanned.get() mustBe 3

    val syncConfig = createConfig(dryRun = false)
    val syncCommand = new SyncCommand(syncConfig)
    Await.result(syncCommand.runSyncJob(), 30.seconds)
    syncCommand.statistics.filesCopied.get() mustBe 3

    val checkConfig = createConfig(dryRun = true)
    SyncLogging.initLogger(checkConfig)
    val checkCommand = new SyncCommand(checkConfig)
    Await.result(checkCommand.runSyncJob(), 30.seconds)
    checkCommand.statistics.filesMissing.get() mustBe 0

    localS3.shutdown()
  }

}
