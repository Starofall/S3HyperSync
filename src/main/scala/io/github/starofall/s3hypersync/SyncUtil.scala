package io.github.starofall.s3hypersync

import com.typesafe.config.{Config, ConfigFactory}
import io.github.starofall.s3hypersync.MainApp.log
import io.github.starofall.s3hypersync.SyncLogging._
import io.github.starofall.s3hypersync.SyncModel.{FileSyncState, SyncStatus}
import org.apache.pekko.actor.{ActorSystem, Cancellable, Scheduler, Terminated}
import org.apache.pekko.pattern.after

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object SyncUtil extends Logger {

  def handleFinalResult(result: Try[_])
                       (implicit actorSystem: ActorSystem): Future[Terminated] = {
    result match {
      case Failure(exception) =>
        log.error("Error Running Sync")
        exception.printStackTrace(System.err)
        actorSystem.registerOnTermination(() => System.exit(1))
        actorSystem.terminate()
      case Success(_)         =>
        actorSystem.registerOnTermination(() => System.exit(0))
        actorSystem.terminate()
    }

  }

  /** if defined, we add a time bomb/timeout to our execution */
  def addTimeoutIfNeeded(conf: JobDefinition)
                        (implicit actorSystem: ActorSystem, exc: ExecutionContext): Option[Cancellable] = {
    // build a time-bomb for timeout
    conf.timeout.toOption.map(timeoutSeconds => {
      log.info("[TIMEOUT]".yellow + s" Set a timeout of $timeoutSeconds seconds".magenta)
      actorSystem.scheduler.scheduleOnce(timeoutSeconds.seconds) {
        log.error("[CRITICAL ERROR] THE PROCESS DID NOT FINISH - HAD TO SELF-KILL".red)
        System.exit(2)
      }
    })
  }


  /** simple retry util in the pekko world */
  def retry[T](retries: Int, delay: FiniteDuration)
              (f: => Future[T])(implicit ec: ExecutionContext, scheduler: Scheduler): Future[T] = {
    f.recoverWith {
      case _ if retries > 0 =>
        log.debug("[RETRY] Had to retry...")
        after(delay, scheduler)(retry(retries - 1, delay)(f))
    }
  }


  /** creates an adjusted config for pekko for our desired worker pool size */
  def buildConfig(conf: JobDefinition): Config = {
    ConfigFactory.parseString(
      s"""pekko {
            loglevel = "ERROR"
            stdout-loglevel = "ERROR"
            actor {
              default-dispatcher {
                type = Dispatcher
                executor = "thread-pool-executor"
                thread-pool-executor {
                  fixed-pool-size = ${conf.numWorkers.getOrElse(64) + 4}
                }
                throughput = 1000
              }
            }
            coordinated-shutdown.log-info = off
            http.host-connection-pool = {
              max-connections = ${conf.numWorkers.getOrElse(64) * 2 + 10}
              max-open-requests = ${conf.numWorkers.getOrElse(64) * 4 + 10}
            }
          }""")
  }



}
