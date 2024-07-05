package io.github.starofall.s3hypersync

import io.github.starofall.s3hypersync.SyncLogUtil.Logger
import io.github.starofall.s3hypersync.SyncModel.{FileSyncState, SyncFile, SyncStatus}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream._
import org.apache.pekko.stream.scaladsl.{GraphDSL, Source}
import org.apache.pekko.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

class PekkoFileSyncCompareStage extends GraphStage[FanInShape2[SyncFile, SyncFile, FileSyncState]] with Logger {

  //@formatter:disable
  val inA  : Inlet[SyncFile]                                = Inlet("CompareAndFilterStage.inA")
  val inB  : Inlet[SyncFile]                                = Inlet("CompareAndFilterStage.inB")
  val out  : Outlet[FileSyncState]                          = Outlet("CompareAndFilterStage.out")
  val shape: FanInShape2[SyncFile, SyncFile, FileSyncState] = new FanInShape2(inA, inB, out)
  //@formatter:enable

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    var aBuffer: Option[SyncFile] = None
    var bBuffer: Option[SyncFile] = None
    var aFinished                 = false
    var bFinished                 = false

    setHandler(inA, new InHandler {
      override def onPush(): Unit = {
        aBuffer = Some(grab(inA))
        log.trace(s"A Pushed - $aBuffer")
        compareAndPush()
      }

      override def onUpstreamFinish(): Unit = {
        log.trace("A upstream finished")
        aFinished = true
        compareAndPush()
      }
    })

    setHandler(inB, new InHandler {
      override def onPush(): Unit = {
        bBuffer = Some(grab(inB))
        log.trace(s"B Pushed - $bBuffer")
        compareAndPush()
      }

      override def onUpstreamFinish(): Unit = {
        log.trace("B upstream finished")
        bFinished = true
        compareAndPush()
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        log.trace("OUT pull")
        if (aBuffer.isEmpty && !hasBeenPulled(inA) && !aFinished) {
          pull(inA)
        }
        if (bBuffer.isEmpty && !hasBeenPulled(inB) && !bFinished) {
          pull(inB)
        }
        compareAndPush()
      }
    })

    def compareAndPush(): Unit = {
      if (isAvailable(out)) {
        (aBuffer, bBuffer) match {
          // if a is slower then b, then a is definitely missing
          case (Some(a), Some(b)) if a.relativeKey < b.relativeKey =>
            log.trace("-> missing")
            push(out, FileSyncState(SyncStatus.Missing, a))
            clearAandPull()

          // if a == b and size is different, call changed
          case (Some(a), Some(b)) if a.relativeKey == b.relativeKey && a.size != b.size =>
            log.trace("-> sizechanged")
            // same but changing etags
            push(out, FileSyncState(SyncStatus.SizeChanged, a))
            clearAandPull()
          //            clearBandPull()

          case (Some(a), Some(b)) if a.relativeKey == b.relativeKey => // aka same size
            log.trace("-> exists")
            // same key, same size
            push(out, FileSyncState(SyncStatus.Exists, a))
            clearAandPull()
          //            clearBandPull()

          // this means a.relativeKey > b.relativeKey aka we need more B to continue
          case (Some(a), Some(b)) if !bFinished =>
            log.trace("-> we need more b")
            clearBandPull()

          // there is a last element in B, but b is done so we just drop it
          case (Some(a), Some(b)) => // aka if bFinished
            log.trace("-> ignore the last b")
            bBuffer = None
            push(out, FileSyncState(SyncStatus.Missing, a))
            clearAandPull()

          // if b is empty AND finished, all other A's are missing
          case (Some(a), None) if bFinished =>
            log.trace("-> b is empty")
            push(out, FileSyncState(SyncStatus.Missing, a))
            clearAandPull()

          // if b is empty but not finished, call for more Bs
          case (Some(a), None) => // aka if !bFinished
            log.trace("-> b empty (but not finished) need more bs")
            clearBandPull()

          // we still have As to call
          case (None, _) if !aFinished =>
            log.trace("-> a empty and more needed")
            clearAandPull()

          // a finished, so we are done
          case (None, _) =>
            log.trace("-> done a empty")
            completeStage()
        }
      }
    }

    private def clearBandPull(): Unit = {
      bBuffer = None
      if (!bFinished && !hasBeenPulled(inB)) pull(inB)
    }

    private def clearAandPull(): Unit = {
      aBuffer = None
      if (!aFinished && !hasBeenPulled(inA)) pull(inA)
    }

    override def preStart(): Unit = {
      pull(inA)
      pull(inB)
    }
  }
}

object PekkoFileSyncCompareStage {

  /** compares the files from the given sources against each other */
  def compareFilesToTarget(syncSource: Source[SyncFile, NotUsed],
                           syncTarget: Source[SyncFile, NotUsed]): Source[FileSyncState, NotUsed] = {
    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val compareAndFilter = builder.add(new PekkoFileSyncCompareStage)
      syncSource ~> compareAndFilter.in0
      syncTarget ~> compareAndFilter.in1
      SourceShape(compareAndFilter.out)
    })
  }

}
